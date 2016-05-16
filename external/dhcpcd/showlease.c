#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>

#include "dhcp.h"
#include "config.h"

#ifndef DEFAULT_LEASETIME
#define DEFAULT_LEASETIME	3600	/* 1 hour */
#endif

#define REQUEST	(1 << 0)
#define UINT8	(1 << 1)
#define UINT16	(1 << 2)
#define SINT16	(1 << 3)
#define UINT32	(1 << 4)
#define SINT32	(1 << 5)
#define IPV4	(1 << 6)
#define STRING	(1 << 7)
#define PAIR	(1 << 8)
#define ARRAY	(1 << 9)
#define RFC3361	(1 << 10)
#define RFC3397	(1 << 11)
#define RFC3442 (1 << 12)

struct dhcp_opt {
	uint8_t option;
	int type;
	const char *var;
};

static const struct dhcp_opt const dhcp_opts[] = {
	{ 1,	IPV4 | REQUEST,	"subnet_mask" },
	{ 2,	UINT32,		"time_offset" },
	{ 3,	IPV4 | ARRAY | REQUEST,	"routers" },
	{ 4,	IPV4 | ARRAY,	"time_servers" },
	{ 5,	IPV4 | ARRAY,	"ien116_name_servers" },
	{ 6,	IPV4 | ARRAY,	"domain_name_servers" },
	{ 7,	IPV4 | ARRAY,	"log_servers" },
	{ 8,	IPV4 | ARRAY,	"cookie_servers" },
	{ 9, 	IPV4 | ARRAY,	"lpr_servers" },
	{ 10,	IPV4 | ARRAY,	"impress_servers" },
	{ 11,	IPV4 | ARRAY,	"resource_location_servers" },
	{ 12,	STRING,		"host_name" },
	{ 13,	UINT16,		"boot_size" },
	{ 14,	STRING,		"merit_dump" },
	{ 15,	STRING,		"domain_name" },
	{ 16,	IPV4,		"swap_server" },
	{ 17,	STRING,		"root_path" },
	{ 18,	STRING,		"extensions_path" },
	{ 19,	UINT8,		"ip_forwarding" },
	{ 20,	UINT8,		"non_local_source_routing" },
	{ 21,	IPV4 | ARRAY,	"policy_filter" },
	{ 22,	SINT16,		"max_dgram_reassembly" },
	{ 23,	UINT16,		"default_ip_ttl" },
	{ 24,	UINT32,		"path_mtu_aging_timeout" },
	{ 25,	UINT16 | ARRAY,	"path_mtu_plateau_table" },
	{ 26,	UINT16,		"interface_mtu" },
	{ 27,	UINT8,		"all_subnets_local" },
	{ 28,	IPV4 | REQUEST,	"broadcast_address" },
	{ 29,	UINT8,		"perform_mask_discovery" },
	{ 30,	UINT8,		"mask_supplier" },
	{ 31,	UINT8,		"router_discovery" },
	{ 32,	IPV4,		"router_solicitation_address" },
	{ 33,	IPV4 | ARRAY | REQUEST,	"static_routes" },
	{ 34,	UINT8,		"trailer_encapsulation" },
	{ 35, 	UINT32,		"arp_cache_timeout" },
	{ 36,	UINT16,		"ieee802_3_encapsulation" },
	{ 37,	UINT8,		"default_tcp_ttl" },
	{ 38,	UINT32,		"tcp_keepalive_interval" },
	{ 39,	UINT8,		"tcp_keepalive_garbage" },
	{ 40,	STRING,		"nis_domain" },
	{ 41,	IPV4 | ARRAY,	"nis_servers" },
	{ 42,	IPV4 | ARRAY,	"ntp_servers" },
	{ 43,	STRING,		"vendor_encapsulated_options" },
	{ 44,	IPV4 | ARRAY,	"netbios_name_servers" },
	{ 45,	IPV4,		"netbios_dd_server" },
	{ 46,	UINT8,		"netbios_node_type" },
	{ 47,	STRING,		"netbios_scope" },
	{ 48,	IPV4 | ARRAY,	"font_servers" },
	{ 49,	IPV4 | ARRAY,	"x_display_manager" },
	{ 50, 	IPV4,		"dhcp_requested_address" },
	{ 51,	UINT32 | REQUEST,	"dhcp_lease_time" },
	{ 52,	UINT8,		"dhcp_option_overload" },
	{ 53,	UINT8,		"dhcp_message_type" },
	{ 54,	IPV4,		"dhcp_server_identifier" },
	{ 55,	UINT8 | ARRAY,	"dhcp_parameter_request_list" },
	{ 56,	STRING,		"dhcp_message" },
	{ 57,	UINT16,		"dhcp_max_message_size" },
	{ 58,	UINT32 | REQUEST,	"dhcp_renewal_time" },
	{ 59,	UINT32 | REQUEST,	"dhcp_rebinding_time" },
	{ 64,	STRING,		"nisplus_domain" },
	{ 65,	IPV4 | ARRAY,	"nisplus_servers" },
	{ 66,	STRING,		"tftp_server_name" },
	{ 67,	STRING,		"bootfile_name" },
	{ 68,	IPV4 | ARRAY,	"mobile_ip_home_agent" },
	{ 69,	IPV4 | ARRAY,	"smtp_server" },
	{ 70,	IPV4 | ARRAY,	"pop_server" },
	{ 71,	IPV4 | ARRAY,	"nntp_server" },
	{ 72,	IPV4 | ARRAY,	"www_server" },
	{ 73,	IPV4 | ARRAY,	"finger_server" },
	{ 74,	IPV4 | ARRAY,	"irc_server" },
	{ 75,	IPV4 | ARRAY,	"streettalk_server" },
	{ 76,	IPV4 | ARRAY,	"streettalk_directory_assistance_server" },
	{ 77,	STRING,		"user_class" },
	{ 85,	IPV4 | ARRAY,	"nds_servers" },
	{ 86,	STRING,		"nds_tree_name" },
	{ 87,	STRING,		"nds_context" },
	{ 88,	STRING | RFC3397,	"bcms_controller_names" },
	{ 89,	IPV4 | ARRAY,	"bcms_controller_address" },
	{ 91,	UINT32,		"client_last_transaction_time" },
	{ 92,	IPV4 | ARRAY,	"associated_ip" },
	{ 98,	STRING,		"uap_servers" },
	{ 112,	IPV4 | ARRAY,	"netinfo_server_address" },
	{ 113,	STRING,		"netinfo_server_tag" },
	{ 114,	STRING,		"default_url" },
	{ 118,	IPV4,		"subnet_selection" },
	{ 119,	STRING | RFC3397,	"domain_search" },
	{ 121,  RFC3442 | REQUEST,	"classless_static_routes" },
	{ 249,  RFC3442,	"ms-classless_static_routes" },
	{ 0, 0, NULL }
};

struct dhcp_message *
get_lease_from_file(const char *leasefile)
{
	int fd;
	struct dhcp_message *dhcp;
	ssize_t bytes;

	fd = open(leasefile, O_RDONLY);
	if (fd == -1)
		return NULL;
	dhcp = malloc(sizeof(*dhcp));
	memset(dhcp, 0, sizeof(*dhcp));
	bytes = read(fd, dhcp, sizeof(*dhcp));
	close(fd);
	if (bytes < 0) {
		free(dhcp);
		dhcp = NULL;
	}
	return dhcp;
}

static uint8_t *dhcp_opt_buffer = NULL;

static int
valid_length(uint8_t option, int dl, int *type)
{
	const struct dhcp_opt *opt;
	ssize_t sz;

	if (dl == 0)
		return -1;

	for (opt = dhcp_opts; opt->option; opt++) {
		if (opt->option != option)
			continue;

		if (type)
			*type = opt->type;

		if (opt->type == 0 || opt->type & STRING || opt->type & RFC3442)
			return 0;

		sz = 0;
		if (opt->type & UINT32 || opt->type & IPV4)
			sz = sizeof(uint32_t);
		if (opt->type & UINT16)
			sz = sizeof(uint16_t);
		if (opt->type & UINT8)
			sz = sizeof(uint8_t);
		if (opt->type & IPV4 || opt->type & ARRAY)
			return dl % sz;
		return (dl == sz ? 0 : -1);
	}

	/* unknown option, so let it pass */
	return 0;
}

static void
free_option_buffer(void)
{
	free(dhcp_opt_buffer);
}


#define get_option_raw(dhcp, opt) get_option(dhcp, opt, NULL, NULL)
static const uint8_t *
get_option(const struct dhcp_message *dhcp, uint8_t opt, int *len, int *type)
{
	const uint8_t *p = dhcp->options;
	const uint8_t *e = p + sizeof(dhcp->options);
	uint8_t l, ol = 0;
	uint8_t o = 0;
	uint8_t overl = 0;
	uint8_t *bp = NULL;
	const uint8_t *op = NULL;
	int bl = 0;

	while (p < e) {
		o = *p++;
		if (o == opt) {
			if (op) {
				if (!dhcp_opt_buffer) {
					dhcp_opt_buffer = malloc(sizeof(struct dhcp_message));
					atexit(free_option_buffer);
				}
				if (!bp) 
					bp = dhcp_opt_buffer;
				memcpy(bp, op, ol);
				bp += ol;
			}
			ol = *p;
			op = p + 1;
			bl += ol;
		}
		switch (o) {
		case DHO_PAD:
			continue;
		case DHO_END:
			if (overl & 1) {
				/* bit 1 set means parse boot file */
				overl &= ~1;
				p = dhcp->bootfile;
				e = p + sizeof(dhcp->bootfile);
			} else if (overl & 2) {
				/* bit 2 set means parse server name */
				overl &= ~2;
				p = dhcp->servername;
				e = p + sizeof(dhcp->servername);
			} else
				goto exit;
			break;
		case DHO_OPTIONSOVERLOADED:
			/* Ensure we only get this option once */
			if (!overl)
				overl = p[1];
			break;
		}
		l = *p++;
		p += l;
	}

exit:
	if (valid_length(o, bl, type) == -1) {
		errno = EINVAL;
		return NULL;
	}
	if (len)
		*len = bl;
	if (bp) {
		memcpy(bp, op, ol);
		return (const uint8_t *)&dhcp_opt_buffer;
	}
	if (op)
		return op;
	errno = ENOENT;
	return NULL;
}

int
get_option_addr32(uint32_t *a, const struct dhcp_message *dhcp, uint8_t option)
{
	const uint8_t *p = get_option_raw(dhcp, option);

	if (!p)
		return -1;
	memcpy(a, p, sizeof(*a));
	return 0;
}

int
get_option_uint32(uint32_t *i, const struct dhcp_message *dhcp, uint8_t option)
{
	uint32_t a;

	if (get_option_addr32(&a, dhcp, option) == -1)
		return -1;

	*i = ntohl(a);
	return 0;
}

uint32_t
get_netmask(uint32_t addr)
{
	uint32_t dst;

	if (addr == 0)
		return 0;

	dst = htonl(addr);
	if (IN_CLASSA(dst))
		return ntohl(IN_CLASSA_NET);
	if (IN_CLASSB (dst))
		return ntohl(IN_CLASSB_NET);
	if (IN_CLASSC (dst))
		return ntohl(IN_CLASSC_NET);

	return 0;
}

void showlease(struct dhcp_lease *lease)
{
    printf("addr:      %s\n", inet_ntoa(lease->addr));
    printf("net:       %s\n", inet_ntoa(lease->net));
    printf("leasetime: %d\n", lease->leasetime);
    printf("renew:     %d\n", lease->renewaltime);
    printf("rebind:    %d\n", lease->rebindtime);
    printf("server:    %s\n", inet_ntoa(lease->server));
}
#define MAX_LEASETIME 2147460

int
main(int argc, char *argv[])
{
    struct dhcp_message *dhcp;
    struct dhcp_lease *lease;
    char leasefile[PATH_MAX];

    if (argc < 2) {
        fprintf(stderr, "Usage: %s <interface>\n", argv[0]);
        exit(1);
    }
    snprintf(leasefile, PATH_MAX, LEASEFILE, argv[1]);
    if ((dhcp = get_lease_from_file(leasefile)) == NULL) {
        fprintf(stderr, "Couldn't read lease file: %s\n", strerror(errno));
        exit(1);
    }
    lease = malloc(sizeof(*lease));
    lease->frominfo = 0;
    lease->addr.s_addr = dhcp->yiaddr;

    if (get_option_addr32(&lease->net.s_addr, dhcp, DHO_SUBNETMASK) == -1)
        lease->net.s_addr = get_netmask(dhcp->yiaddr);
    if (get_option_uint32(&lease->leasetime, dhcp, DHO_LEASETIME) != 0)
        lease->leasetime = DEFAULT_LEASETIME;
    get_option_addr32(&lease->server.s_addr, dhcp, DHO_SERVERID);
    /* Dm: limit lease time value to avoid negative numbers when
       converting to milliseconds */		
    if ((lease->leasetime != ~0U) && (lease->leasetime > MAX_LEASETIME))
        lease->leasetime = MAX_LEASETIME;
    if (get_option_uint32(&lease->renewaltime, dhcp, DHO_RENEWALTIME) != 0)
        lease->renewaltime = 0;
    if (get_option_uint32(&lease->rebindtime, dhcp, DHO_REBINDTIME) != 0)
        lease->rebindtime = 0;
    showlease(lease);
    free(lease);
    return 0;
}
