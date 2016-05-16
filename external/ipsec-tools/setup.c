/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/param.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <netdb.h>
#include <fcntl.h>

#include "config.h"
#include "gcmalloc.h"
#include "libpfkey.h"
#include "var.h"
#include "isakmp_var.h"
#include "isakmp.h"
#include "isakmp_xauth.h"
#include "vmbuf.h"
#include "crypto_openssl.h"
#include "oakley.h"
#include "ipsec_doi.h"
#include "algorithm.h"
#include "vendorid.h"
#include "schedule.h"
#include "pfkey.h"
#include "nattraversal.h"
#include "proposal.h"
#include "sainfo.h"
#include "localconf.h"
#include "remoteconf.h"
#include "sockmisc.h"
#include "grabmyaddr.h"
#include "plog.h"
#include "admin.h"
#include "privsep.h"
#include "throttle.h"
#include "misc.h"

static struct localconf localconf;
static struct sainfo sainfo;
static char *pre_shared_key;

static struct sockaddr *targets[2];
static struct sockaddr *source;
static struct myaddrs myaddrs[2];

struct localconf *lcconf = &localconf;
int f_local = 0;

/*****************************************************************************/

static void add_sainfo_algorithm(int class, int algorithm, int length)
{
    struct sainfoalg *p = calloc(1, sizeof(struct sainfoalg));
    p->alg = algorithm;
    p->encklen = length;

    if (!sainfo.algs[class]) {
        sainfo.algs[class] = p;
    } else {
        struct sainfoalg *q = sainfo.algs[class];
        while (q->next) {
            q = q->next;
        }
        q->next = p;
    }
}

static void set_globals(char *server)
{
    struct addrinfo hints = {
        .ai_flags = AI_NUMERICSERV,
#ifndef INET6
        .ai_family = AF_INET,
#else
        .ai_family = AF_UNSPEC,
#endif
        .ai_socktype = SOCK_DGRAM,
    };
    struct addrinfo *info;

    if (getaddrinfo(server, "500", &hints, &info) != 0) {
        do_plog(LLV_ERROR, "Cannot resolve address: %s\n", server);
        exit(1);
    }
    if (info->ai_next) {
        do_plog(LLV_WARNING, "Found multiple addresses. Use the first one.\n");
    }
    targets[0] = dupsaddr(info->ai_addr);
    freeaddrinfo(info);

    source = getlocaladdr(targets[0]);
    if (!source) {
        do_plog(LLV_ERROR, "Cannot get local address\n");
        exit(1);
    }
    set_port(targets[0], 0);
    set_port(source, 0);

    myaddrs[0].addr = dupsaddr(source);
    set_port(myaddrs[0].addr, PORT_ISAKMP);
    myaddrs[0].sock = -1;
#ifdef ENABLE_NATT
    myaddrs[0].next = &myaddrs[1];
    myaddrs[1].addr = dupsaddr(myaddrs[0].addr);
    set_port(myaddrs[1].addr, PORT_ISAKMP_NATT);
    myaddrs[1].sock = -1;
    myaddrs[1].udp_encap = 1;
#endif

    localconf.myaddrs = &myaddrs[0];
    localconf.port_isakmp = PORT_ISAKMP;
    localconf.port_isakmp_natt = PORT_ISAKMP_NATT;
    localconf.default_af = AF_INET;
    localconf.pathinfo[LC_PATHTYPE_CERT] = "./";
    localconf.pad_random = LC_DEFAULT_PAD_RANDOM;
    localconf.pad_randomlen = LC_DEFAULT_PAD_RANDOM;
    localconf.pad_strict = LC_DEFAULT_PAD_STRICT;
    localconf.pad_excltail = LC_DEFAULT_PAD_EXCLTAIL;
    localconf.retry_counter = 10;
    localconf.retry_interval = 3;
    localconf.count_persend = LC_DEFAULT_COUNT_PERSEND;
    localconf.secret_size = LC_DEFAULT_SECRETSIZE;
    localconf.retry_checkph1 = LC_DEFAULT_RETRY_CHECKPH1;
    localconf.wait_ph2complete = LC_DEFAULT_WAIT_PH2COMPLETE;
    localconf.natt_ka_interval = LC_DEFAULT_NATT_KA_INTERVAL;

    sainfo.lifetime = IPSECDOI_ATTR_SA_LD_SEC_DEFAULT;
    sainfo.lifebyte = IPSECDOI_ATTR_SA_LD_KB_MAX;
    add_sainfo_algorithm(algclass_ipsec_auth, IPSECDOI_ATTR_AUTH_HMAC_SHA1, 0);
    add_sainfo_algorithm(algclass_ipsec_auth, IPSECDOI_ATTR_AUTH_HMAC_MD5, 0);
    add_sainfo_algorithm(algclass_ipsec_enc, IPSECDOI_ESP_AES, 256);
    add_sainfo_algorithm(algclass_ipsec_enc, IPSECDOI_ESP_AES, 128);
    add_sainfo_algorithm(algclass_ipsec_enc, IPSECDOI_ESP_3DES, 0);
    add_sainfo_algorithm(algclass_ipsec_enc, IPSECDOI_ESP_DES, 0);

    memset(script_names, 0, sizeof(script_names));
}

/*****************************************************************************/

static int policy_match(struct sadb_address *address)
{
    if (address) {
        struct sockaddr *addr = PFKEY_ADDR_SADDR(address);
        return !cmpsaddrwop(addr, targets[0]) || !cmpsaddrwop(addr, targets[1]);
    }
    return 0;
}

/* flush; spdflush; */
static void flush()
{
    struct sadb_msg *p;
    int replies = 0;
    int key = pfkey_open();

    if (pfkey_send_dump(key, SADB_SATYPE_UNSPEC) <= 0 ||
        pfkey_send_spddump(key) <= 0) {
        do_plog(LLV_ERROR, "Cannot dump SAD and SPD\n");
        exit(1);
    }

    for (p = NULL; replies < 2 && (p = pfkey_recv(key)) != NULL; free(p)) {
        caddr_t q[SADB_EXT_MAX + 1];

        if (p->sadb_msg_type != SADB_DUMP &&
            p->sadb_msg_type != SADB_X_SPDDUMP) {
            continue;
        }
        replies += !p->sadb_msg_seq;

        if (p->sadb_msg_errno || pfkey_align(p, q) || pfkey_check(q)) {
            continue;
        }
        if (policy_match((struct sadb_address *)q[SADB_EXT_ADDRESS_SRC]) ||
            policy_match((struct sadb_address *)q[SADB_EXT_ADDRESS_DST])) {
            p->sadb_msg_type = (p->sadb_msg_type == SADB_DUMP) ?
                               SADB_DELETE : SADB_X_SPDDELETE;
            p->sadb_msg_reserved = 0;
            p->sadb_msg_seq = 0;
            pfkey_send(key, p, PFKEY_UNUNIT64(p->sadb_msg_len));
        }
    }

    pfkey_close(key);
}

/* spdadd src dst protocol -P out ipsec esp/transport//require;
 * spdadd dst src protocol -P in  ipsec esp/transport//require;
 * or
 * spdadd src any protocol -P out ipsec esp/tunnel/local-remote/require;
 * spdadd any src protocol -P in  ipsec esp/tunnel/remote-local/require; */
static void spdadd(struct sockaddr *src, struct sockaddr *dst,
        int protocol, struct sockaddr *local, struct sockaddr *remote)
{
    struct __attribute__((packed)) {
        struct sadb_x_policy p;
        struct sadb_x_ipsecrequest q;
        char addresses[sizeof(struct sockaddr_storage) * 2];
    } policy;

    struct sockaddr_storage any = {
#ifndef __linux__
        .ss_len = src->sa_len,
#endif
        .ss_family = src->sa_family,
    };

    int src_prefix = (src->sa_family == AF_INET) ? 32 : 128;
    int dst_prefix = src_prefix;
    int length = 0;
    int key;

    /* Fill values for outbound policy. */
    memset(&policy, 0, sizeof(policy));
    policy.p.sadb_x_policy_exttype = SADB_X_EXT_POLICY;
    policy.p.sadb_x_policy_type = IPSEC_POLICY_IPSEC;
    policy.p.sadb_x_policy_dir = IPSEC_DIR_OUTBOUND;
#ifdef HAVE_PFKEY_POLICY_PRIORITY
    policy.p.sadb_x_policy_priority = PRIORITY_DEFAULT;
#endif
    policy.q.sadb_x_ipsecrequest_proto = IPPROTO_ESP;
    policy.q.sadb_x_ipsecrequest_mode = IPSEC_MODE_TRANSPORT;
    policy.q.sadb_x_ipsecrequest_level = IPSEC_LEVEL_REQUIRE;

    /* Deal with tunnel mode. */
    if (!dst) {
        int size = sysdep_sa_len(local);
        memcpy(policy.addresses, local, size);
        memcpy(&policy.addresses[size], remote, size);
        length += size + size;

        policy.q.sadb_x_ipsecrequest_mode = IPSEC_MODE_TUNNEL;
        dst = (struct sockaddr *)&any;
        dst_prefix = 0;

        /* Also use the source address to filter policies. */
        targets[1] = dupsaddr(src);
    }

    /* Fix lengths. */
    length += sizeof(policy.q);
    policy.q.sadb_x_ipsecrequest_len = length;
    length += sizeof(policy.p);
    policy.p.sadb_x_policy_len = PFKEY_UNIT64(length);

    /* Always do a flush before adding new policies. */
    flush();

    /* Set outbound policy. */
    key = pfkey_open();
    if (pfkey_send_spdadd(key, src, src_prefix, dst, dst_prefix, protocol,
            (caddr_t)&policy, length, 0) <= 0) {
        do_plog(LLV_ERROR, "Cannot set outbound policy\n");
        exit(1);
    }

    /* Flip values for inbound policy. */
    policy.p.sadb_x_policy_dir = IPSEC_DIR_INBOUND;
    if (!dst_prefix) {
        int size = sysdep_sa_len(local);
        memcpy(policy.addresses, remote, size);
        memcpy(&policy.addresses[size], local, size);
    }

    /* Set inbound policy. */
    if (pfkey_send_spdadd(key, dst, dst_prefix, src, src_prefix, protocol,
            (caddr_t)&policy, length, 0) <= 0) {
        do_plog(LLV_ERROR, "Cannot set inbound policy\n");
        exit(1);
    }

    pfkey_close(key);
    atexit(flush);
}

/*****************************************************************************/

static void add_proposal(struct remoteconf *remoteconf,
        int auth, int hash, int encryption, int length)
{
    struct isakmpsa *p = racoon_calloc(1, sizeof(struct isakmpsa));
    p->prop_no = 1;
    p->lifetime = OAKLEY_ATTR_SA_LD_SEC_DEFAULT;
    p->enctype = encryption;
    p->encklen = length;
    p->authmethod = auth;
    p->hashtype = hash;
    p->dh_group = OAKLEY_ATTR_GRP_DESC_MODP1024;
    p->vendorid = VENDORID_UNKNOWN;
    p->rmconf = remoteconf;

    if (!remoteconf->proposal) {
      p->trns_no = 1;
      remoteconf->proposal = p;
    } else {
        struct isakmpsa *q = remoteconf->proposal;
        while (q->next) {
            q = q->next;
        }
        p->trns_no = q->trns_no + 1;
        q->next = p;
    }
}

static vchar_t *strtovchar(char *string)
{
    vchar_t *vchar = string ? vmalloc(strlen(string) + 1) : NULL;
    if (vchar) {
        memcpy(vchar->v, string, vchar->l);
        vchar->l -= 1;
    }
    return vchar;
}

static void set_pre_shared_key(struct remoteconf *remoteconf,
        char *identifier, char *key)
{
    pre_shared_key = key;
    if (identifier[0]) {
        remoteconf->idv = strtovchar(identifier);
        remoteconf->etypes->type = ISAKMP_ETYPE_AGG;

        remoteconf->idvtype = IDTYPE_KEYID;
        if (strchr(identifier, '.')) {
            remoteconf->idvtype = IDTYPE_FQDN;
            if (strchr(identifier, '@')) {
                remoteconf->idvtype = IDTYPE_USERFQDN;
            }
        }
    }
}

static void set_certificates(struct remoteconf *remoteconf,
        char *user_private_key, char *user_certificate,
        char *ca_certificate, char *server_certificate)
{
    remoteconf->myprivfile = user_private_key;
    remoteconf->mycertfile = user_certificate;
    if (user_certificate) {
        remoteconf->idvtype = IDTYPE_ASN1DN;
    }
    if (!ca_certificate[0]) {
        remoteconf->verify_cert = FALSE;
    } else {
        remoteconf->cacertfile = ca_certificate;
    }
    if (server_certificate[0]) {
        remoteconf->peerscertfile = server_certificate;
        remoteconf->getcert_method = ISAKMP_GETCERT_LOCALFILE;
    }
}

#ifdef ENABLE_HYBRID

static void set_xauth_and_more(struct remoteconf *remoteconf,
        char *username, char *password, char *phase1_up, char *script_arg)
{
    struct xauth_rmconf *xauth = racoon_calloc(1, sizeof(struct xauth_rmconf));
    xauth->login = strtovchar(username);
    xauth->login->l += 1;
    xauth->pass = strtovchar(password);
    xauth->pass->l += 1;
    remoteconf->xauth = xauth;
    remoteconf->mode_cfg = TRUE;
    remoteconf->script[SCRIPT_PHASE1_UP] = strtovchar(phase1_up);
    script_names[SCRIPT_PHASE1_UP] = script_arg;
}

#endif

extern void monitor_fd(int fd, void (*callback)(int));

void add_isakmp_handler(int fd, const char *interface)
{
    if (setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE,
            interface, strlen(interface))) {
        do_plog(LLV_WARNING, "Cannot bind socket to %s\n", interface);
    }
    monitor_fd(fd, (void *)isakmp_handler);
}

void setup(int argc, char **argv)
{
    struct remoteconf *remoteconf = NULL;
    int auth;

    if (argc > 2) {
        set_globals(argv[2]);

        /* Initialize everything else. */
        eay_init();
        initrmconf();
        oakley_dhinit();
        compute_vendorids();
        sched_init();
        if (pfkey_init() < 0 || isakmp_init() < 0) {
            exit(1);
        }
        monitor_fd(localconf.sock_pfkey, (void *)pfkey_handler);
        add_isakmp_handler(myaddrs[0].sock, argv[1]);
#ifdef ENABLE_NATT
        add_isakmp_handler(myaddrs[1].sock, argv[1]);
        natt_keepalive_init();
#endif

        /* Create remote configuration. */
        remoteconf = newrmconf();
        remoteconf->etypes = racoon_calloc(1, sizeof(struct etypes));
        remoteconf->etypes->type = ISAKMP_ETYPE_IDENT;
        remoteconf->idvtype = IDTYPE_ADDRESS;
        remoteconf->ike_frag = TRUE;
        remoteconf->pcheck_level = PROP_CHECK_CLAIM;
        remoteconf->certtype = ISAKMP_CERT_X509SIGN;
        remoteconf->gen_policy = TRUE;
        remoteconf->nat_traversal = TRUE;
        remoteconf->dh_group = OAKLEY_ATTR_GRP_DESC_MODP1024;
        remoteconf->script[SCRIPT_PHASE1_UP] = strtovchar("");
        remoteconf->script[SCRIPT_PHASE1_DOWN] = strtovchar("");
        oakley_setdhgroup(remoteconf->dh_group, &remoteconf->dhgrp);
        remoteconf->remote = dupsaddr(targets[0]);
    }

    /* Set authentication method and credentials. */
    if (argc == 7 && !strcmp(argv[3], "udppsk")) {
        set_pre_shared_key(remoteconf, argv[4], argv[5]);
        auth = OAKLEY_ATTR_AUTH_METHOD_PSKEY;

        set_port(targets[0], atoi(argv[6]));
        spdadd(source, targets[0], IPPROTO_UDP, NULL, NULL);
    } else if (argc == 9 && !strcmp(argv[3], "udprsa")) {
        set_certificates(remoteconf, argv[4], argv[5], argv[6], argv[7]);
        auth = OAKLEY_ATTR_AUTH_METHOD_RSASIG;

        set_port(targets[0], atoi(argv[8]));
        spdadd(source, targets[0], IPPROTO_UDP, NULL, NULL);
#ifdef ENABLE_HYBRID
    } else if (argc == 10 && !strcmp(argv[3], "xauthpsk")) {
        set_pre_shared_key(remoteconf, argv[4], argv[5]);
        set_xauth_and_more(remoteconf, argv[6], argv[7], argv[8], argv[9]);
        auth = OAKLEY_ATTR_AUTH_METHOD_XAUTH_PSKEY_I;
    } else if (argc == 12 && !strcmp(argv[3], "xauthrsa")) {
        set_certificates(remoteconf, argv[4], argv[5], argv[6], argv[7]);
        set_xauth_and_more(remoteconf, argv[8], argv[9], argv[10], argv[11]);
        auth = OAKLEY_ATTR_AUTH_METHOD_XAUTH_RSASIG_I;
    } else if (argc == 10 && !strcmp(argv[3], "hybridrsa")) {
        set_certificates(remoteconf, NULL, NULL, argv[4], argv[5]);
        set_xauth_and_more(remoteconf, argv[6], argv[7], argv[8], argv[9]);
        auth = OAKLEY_ATTR_AUTH_METHOD_HYBRID_RSA_I;
#endif
    } else {
        printf("Usage: %s <interface> <server> [...], where [...] can be:\n"
                " udppsk    <identifier> <pre-shared-key> <port>; \n"
                " udprsa    <user-private-key> <user-certificate> \\\n"
                "           <ca-certificate> <server-certificate> <port>;\n"
#ifdef ENABLE_HYBRID
                " xauthpsk  <identifier> <pre-shared-key> \\\n"
                "           <username> <password> <phase1-up> <script-arg>;\n"
                " xauthrsa  <user-private-key> <user-certificate> \\\n"
                "           <ca-certificate> <server-certificate> \\\n"
                "           <username> <password> <phase1-up> <script-arg>;\n"
                " hybridrsa <ca-certificate> <server-certificate> \\\n"
                "           <username> <password> <phase1-up> <script-arg>;\n"
#endif
                "", argv[0]);
        exit(0);
    }

    /* Add proposals. */
    add_proposal(remoteconf, auth,
            OAKLEY_ATTR_HASH_ALG_SHA, OAKLEY_ATTR_ENC_ALG_AES, 256);
    add_proposal(remoteconf, auth,
            OAKLEY_ATTR_HASH_ALG_MD5, OAKLEY_ATTR_ENC_ALG_AES, 256);
    add_proposal(remoteconf, auth,
            OAKLEY_ATTR_HASH_ALG_SHA, OAKLEY_ATTR_ENC_ALG_AES, 128);
    add_proposal(remoteconf, auth,
            OAKLEY_ATTR_HASH_ALG_MD5, OAKLEY_ATTR_ENC_ALG_AES, 128);
    add_proposal(remoteconf, auth,
            OAKLEY_ATTR_HASH_ALG_SHA, OAKLEY_ATTR_ENC_ALG_3DES, 0);
    add_proposal(remoteconf, auth,
            OAKLEY_ATTR_HASH_ALG_MD5, OAKLEY_ATTR_ENC_ALG_3DES, 0);
    add_proposal(remoteconf, auth,
            OAKLEY_ATTR_HASH_ALG_SHA, OAKLEY_ATTR_ENC_ALG_DES, 0);
    add_proposal(remoteconf, auth,
            OAKLEY_ATTR_HASH_ALG_MD5, OAKLEY_ATTR_ENC_ALG_DES, 0);

    /* Install remote configuration. */
    insrmconf(remoteconf);

    /* Start phase 1 negotiation for xauth. */
    if (remoteconf->xauth) {
        isakmp_ph1begin_i(remoteconf, remoteconf->remote, source);
    }
}

/*****************************************************************************/

/* localconf.h */

vchar_t *getpskbyaddr(struct sockaddr *addr)
{
    return strtovchar(pre_shared_key);
}

vchar_t *getpskbyname(vchar_t *name)
{
    return NULL;
}

void getpathname(char *path, int length, int type, const char *name)
{
    if (pname) {
        snprintf(path, length, pname, name);
    } else {
        strncpy(path, name, length);
    }
    path[length - 1] = '\0';
}

/* grabmyaddr.h */

int myaddr_getsport(struct sockaddr *addr)
{
    return 0;
}

int getsockmyaddr(struct sockaddr *addr)
{
#ifdef ENABLE_NATT
    if (!cmpsaddrstrict(addr, myaddrs[1].addr)) {
        return myaddrs[1].sock;
    }
#endif
    if (!cmpsaddrwop(addr, myaddrs[0].addr)) {
        return myaddrs[0].sock;
    }
    return -1;
}

/* privsep.h */

int privsep_pfkey_open()
{
    return pfkey_open();
}

void privsep_pfkey_close(int key)
{
    pfkey_close(key);
}

vchar_t *privsep_eay_get_pkcs1privkey(char *file)
{
    return eay_get_pkcs1privkey(file);
}

static char *get_env(char * const *envp, char *key)
{
    int length = strlen(key);
    while (*envp && (strncmp(*envp, key, length) || (*envp)[length] != '=')) {
        ++envp;
    }
    return *envp ? &(*envp)[length + 1] : "";
}

static int skip_script = 0;
extern const char *android_hook(char **envp);

int privsep_script_exec(char *script, int name, char * const *envp)
{
    if (skip_script) {
        return 0;
    }
    skip_script = 1;

    if (name == SCRIPT_PHASE1_DOWN) {
        exit(1);
    }
    if (script_names[SCRIPT_PHASE1_UP]) {
        /* Racoon ignores INTERNAL_IP6_ADDRESS, so we only do IPv4. */
        struct sockaddr *addr4 = str2saddr(get_env(envp, "INTERNAL_ADDR4"),
                NULL);
        struct sockaddr *local = str2saddr(get_env(envp, "LOCAL_ADDR"),
                get_env(envp, "LOCAL_PORT"));
        struct sockaddr *remote = str2saddr(get_env(envp, "REMOTE_ADDR"),
                get_env(envp, "REMOTE_PORT"));

        if (addr4 && local && remote) {
#ifdef ANDROID_CHANGES
            if (pname) {
                script = (char *)android_hook((char **)envp);
            }
#endif
            spdadd(addr4, NULL, IPPROTO_IP, local, remote);
        } else {
            do_plog(LLV_ERROR, "Cannot get parameters for SPD policy.\n");
            exit(1);
        }

        racoon_free(addr4);
        racoon_free(local);
        racoon_free(remote);
        return script_exec(script, name, envp);
    }
    return 0;
}

int privsep_accounting_system(int port, struct sockaddr *addr,
        char *user, int status)
{
    return 0;
}

int privsep_xauth_login_system(char *user, char *password)
{
    return -1;
}

/* misc.h */

int racoon_hexdump(void *data, size_t length)
{
    return 0;
}

/* sainfo.h */

struct sainfo *getsainfo(const vchar_t *src, const vchar_t *dst,
        const vchar_t *peer, int remoteid)
{
    return &sainfo;
}

const char *sainfo2str(const struct sainfo *si)
{
    return "*";
}

/* throttle.h */

int throttle_host(struct sockaddr *addr, int fail)
{
    return 0;
}
