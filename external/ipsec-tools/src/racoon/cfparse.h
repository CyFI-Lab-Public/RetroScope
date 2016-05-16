/* A Bison parser, made by GNU Bison 2.3.  */

/* Skeleton interface for Bison's Yacc-like parsers in C

   Copyright (C) 1984, 1989, 1990, 2000, 2001, 2002, 2003, 2004, 2005, 2006
   Free Software Foundation, Inc.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor,
   Boston, MA 02110-1301, USA.  */

/* As a special exception, you may create a larger work that contains
   part or all of the Bison parser skeleton and distribute that work
   under terms of your choice, so long as that work isn't itself a
   parser generator using the skeleton or a modified version thereof
   as a parser skeleton.  Alternatively, if you modify or redistribute
   the parser skeleton itself, you may (at your option) remove this
   special exception, which will cause the skeleton and the resulting
   Bison output files to be licensed under the GNU General Public
   License without this special exception.

   This special exception was added by the Free Software Foundation in
   version 2.2 of Bison.  */

/* Tokens.  */
#ifndef YYTOKENTYPE
# define YYTOKENTYPE
   /* Put the tokens into the symbol table, so that GDB and other debuggers
      know about them.  */
   enum yytokentype {
     PRIVSEP = 258,
     USER = 259,
     GROUP = 260,
     CHROOT = 261,
     PATH = 262,
     PATHTYPE = 263,
     INCLUDE = 264,
     IDENTIFIER = 265,
     VENDORID = 266,
     LOGGING = 267,
     LOGLEV = 268,
     PADDING = 269,
     PAD_RANDOMIZE = 270,
     PAD_RANDOMIZELEN = 271,
     PAD_MAXLEN = 272,
     PAD_STRICT = 273,
     PAD_EXCLTAIL = 274,
     LISTEN = 275,
     X_ISAKMP = 276,
     X_ISAKMP_NATT = 277,
     X_ADMIN = 278,
     STRICT_ADDRESS = 279,
     ADMINSOCK = 280,
     DISABLED = 281,
     LDAPCFG = 282,
     LDAP_HOST = 283,
     LDAP_PORT = 284,
     LDAP_PVER = 285,
     LDAP_BASE = 286,
     LDAP_BIND_DN = 287,
     LDAP_BIND_PW = 288,
     LDAP_SUBTREE = 289,
     LDAP_ATTR_USER = 290,
     LDAP_ATTR_ADDR = 291,
     LDAP_ATTR_MASK = 292,
     LDAP_ATTR_GROUP = 293,
     LDAP_ATTR_MEMBER = 294,
     MODECFG = 295,
     CFG_NET4 = 296,
     CFG_MASK4 = 297,
     CFG_DNS4 = 298,
     CFG_NBNS4 = 299,
     CFG_DEFAULT_DOMAIN = 300,
     CFG_AUTH_SOURCE = 301,
     CFG_AUTH_GROUPS = 302,
     CFG_SYSTEM = 303,
     CFG_RADIUS = 304,
     CFG_PAM = 305,
     CFG_LDAP = 306,
     CFG_LOCAL = 307,
     CFG_NONE = 308,
     CFG_GROUP_SOURCE = 309,
     CFG_ACCOUNTING = 310,
     CFG_CONF_SOURCE = 311,
     CFG_MOTD = 312,
     CFG_POOL_SIZE = 313,
     CFG_AUTH_THROTTLE = 314,
     CFG_SPLIT_NETWORK = 315,
     CFG_SPLIT_LOCAL = 316,
     CFG_SPLIT_INCLUDE = 317,
     CFG_SPLIT_DNS = 318,
     CFG_PFS_GROUP = 319,
     CFG_SAVE_PASSWD = 320,
     RETRY = 321,
     RETRY_COUNTER = 322,
     RETRY_INTERVAL = 323,
     RETRY_PERSEND = 324,
     RETRY_PHASE1 = 325,
     RETRY_PHASE2 = 326,
     NATT_KA = 327,
     ALGORITHM_CLASS = 328,
     ALGORITHMTYPE = 329,
     STRENGTHTYPE = 330,
     SAINFO = 331,
     FROM = 332,
     REMOTE = 333,
     ANONYMOUS = 334,
     INHERIT = 335,
     EXCHANGE_MODE = 336,
     EXCHANGETYPE = 337,
     DOI = 338,
     DOITYPE = 339,
     SITUATION = 340,
     SITUATIONTYPE = 341,
     CERTIFICATE_TYPE = 342,
     CERTTYPE = 343,
     PEERS_CERTFILE = 344,
     CA_TYPE = 345,
     VERIFY_CERT = 346,
     SEND_CERT = 347,
     SEND_CR = 348,
     IDENTIFIERTYPE = 349,
     IDENTIFIERQUAL = 350,
     MY_IDENTIFIER = 351,
     PEERS_IDENTIFIER = 352,
     VERIFY_IDENTIFIER = 353,
     DNSSEC = 354,
     CERT_X509 = 355,
     CERT_PLAINRSA = 356,
     NONCE_SIZE = 357,
     DH_GROUP = 358,
     KEEPALIVE = 359,
     PASSIVE = 360,
     INITIAL_CONTACT = 361,
     NAT_TRAVERSAL = 362,
     REMOTE_FORCE_LEVEL = 363,
     PROPOSAL_CHECK = 364,
     PROPOSAL_CHECK_LEVEL = 365,
     GENERATE_POLICY = 366,
     GENERATE_LEVEL = 367,
     SUPPORT_PROXY = 368,
     PROPOSAL = 369,
     EXEC_PATH = 370,
     EXEC_COMMAND = 371,
     EXEC_SUCCESS = 372,
     EXEC_FAILURE = 373,
     GSS_ID = 374,
     GSS_ID_ENC = 375,
     GSS_ID_ENCTYPE = 376,
     COMPLEX_BUNDLE = 377,
     DPD = 378,
     DPD_DELAY = 379,
     DPD_RETRY = 380,
     DPD_MAXFAIL = 381,
     PH1ID = 382,
     XAUTH_LOGIN = 383,
     WEAK_PHASE1_CHECK = 384,
     PREFIX = 385,
     PORT = 386,
     PORTANY = 387,
     UL_PROTO = 388,
     ANY = 389,
     IKE_FRAG = 390,
     ESP_FRAG = 391,
     MODE_CFG = 392,
     PFS_GROUP = 393,
     LIFETIME = 394,
     LIFETYPE_TIME = 395,
     LIFETYPE_BYTE = 396,
     STRENGTH = 397,
     REMOTEID = 398,
     SCRIPT = 399,
     PHASE1_UP = 400,
     PHASE1_DOWN = 401,
     NUMBER = 402,
     SWITCH = 403,
     BOOLEAN = 404,
     HEXSTRING = 405,
     QUOTEDSTRING = 406,
     ADDRSTRING = 407,
     ADDRRANGE = 408,
     UNITTYPE_BYTE = 409,
     UNITTYPE_KBYTES = 410,
     UNITTYPE_MBYTES = 411,
     UNITTYPE_TBYTES = 412,
     UNITTYPE_SEC = 413,
     UNITTYPE_MIN = 414,
     UNITTYPE_HOUR = 415,
     EOS = 416,
     BOC = 417,
     EOC = 418,
     COMMA = 419
   };
#endif
/* Tokens.  */
#define PRIVSEP 258
#define USER 259
#define GROUP 260
#define CHROOT 261
#define PATH 262
#define PATHTYPE 263
#define INCLUDE 264
#define IDENTIFIER 265
#define VENDORID 266
#define LOGGING 267
#define LOGLEV 268
#define PADDING 269
#define PAD_RANDOMIZE 270
#define PAD_RANDOMIZELEN 271
#define PAD_MAXLEN 272
#define PAD_STRICT 273
#define PAD_EXCLTAIL 274
#define LISTEN 275
#define X_ISAKMP 276
#define X_ISAKMP_NATT 277
#define X_ADMIN 278
#define STRICT_ADDRESS 279
#define ADMINSOCK 280
#define DISABLED 281
#define LDAPCFG 282
#define LDAP_HOST 283
#define LDAP_PORT 284
#define LDAP_PVER 285
#define LDAP_BASE 286
#define LDAP_BIND_DN 287
#define LDAP_BIND_PW 288
#define LDAP_SUBTREE 289
#define LDAP_ATTR_USER 290
#define LDAP_ATTR_ADDR 291
#define LDAP_ATTR_MASK 292
#define LDAP_ATTR_GROUP 293
#define LDAP_ATTR_MEMBER 294
#define MODECFG 295
#define CFG_NET4 296
#define CFG_MASK4 297
#define CFG_DNS4 298
#define CFG_NBNS4 299
#define CFG_DEFAULT_DOMAIN 300
#define CFG_AUTH_SOURCE 301
#define CFG_AUTH_GROUPS 302
#define CFG_SYSTEM 303
#define CFG_RADIUS 304
#define CFG_PAM 305
#define CFG_LDAP 306
#define CFG_LOCAL 307
#define CFG_NONE 308
#define CFG_GROUP_SOURCE 309
#define CFG_ACCOUNTING 310
#define CFG_CONF_SOURCE 311
#define CFG_MOTD 312
#define CFG_POOL_SIZE 313
#define CFG_AUTH_THROTTLE 314
#define CFG_SPLIT_NETWORK 315
#define CFG_SPLIT_LOCAL 316
#define CFG_SPLIT_INCLUDE 317
#define CFG_SPLIT_DNS 318
#define CFG_PFS_GROUP 319
#define CFG_SAVE_PASSWD 320
#define RETRY 321
#define RETRY_COUNTER 322
#define RETRY_INTERVAL 323
#define RETRY_PERSEND 324
#define RETRY_PHASE1 325
#define RETRY_PHASE2 326
#define NATT_KA 327
#define ALGORITHM_CLASS 328
#define ALGORITHMTYPE 329
#define STRENGTHTYPE 330
#define SAINFO 331
#define FROM 332
#define REMOTE 333
#define ANONYMOUS 334
#define INHERIT 335
#define EXCHANGE_MODE 336
#define EXCHANGETYPE 337
#define DOI 338
#define DOITYPE 339
#define SITUATION 340
#define SITUATIONTYPE 341
#define CERTIFICATE_TYPE 342
#define CERTTYPE 343
#define PEERS_CERTFILE 344
#define CA_TYPE 345
#define VERIFY_CERT 346
#define SEND_CERT 347
#define SEND_CR 348
#define IDENTIFIERTYPE 349
#define IDENTIFIERQUAL 350
#define MY_IDENTIFIER 351
#define PEERS_IDENTIFIER 352
#define VERIFY_IDENTIFIER 353
#define DNSSEC 354
#define CERT_X509 355
#define CERT_PLAINRSA 356
#define NONCE_SIZE 357
#define DH_GROUP 358
#define KEEPALIVE 359
#define PASSIVE 360
#define INITIAL_CONTACT 361
#define NAT_TRAVERSAL 362
#define REMOTE_FORCE_LEVEL 363
#define PROPOSAL_CHECK 364
#define PROPOSAL_CHECK_LEVEL 365
#define GENERATE_POLICY 366
#define GENERATE_LEVEL 367
#define SUPPORT_PROXY 368
#define PROPOSAL 369
#define EXEC_PATH 370
#define EXEC_COMMAND 371
#define EXEC_SUCCESS 372
#define EXEC_FAILURE 373
#define GSS_ID 374
#define GSS_ID_ENC 375
#define GSS_ID_ENCTYPE 376
#define COMPLEX_BUNDLE 377
#define DPD 378
#define DPD_DELAY 379
#define DPD_RETRY 380
#define DPD_MAXFAIL 381
#define PH1ID 382
#define XAUTH_LOGIN 383
#define WEAK_PHASE1_CHECK 384
#define PREFIX 385
#define PORT 386
#define PORTANY 387
#define UL_PROTO 388
#define ANY 389
#define IKE_FRAG 390
#define ESP_FRAG 391
#define MODE_CFG 392
#define PFS_GROUP 393
#define LIFETIME 394
#define LIFETYPE_TIME 395
#define LIFETYPE_BYTE 396
#define STRENGTH 397
#define REMOTEID 398
#define SCRIPT 399
#define PHASE1_UP 400
#define PHASE1_DOWN 401
#define NUMBER 402
#define SWITCH 403
#define BOOLEAN 404
#define HEXSTRING 405
#define QUOTEDSTRING 406
#define ADDRSTRING 407
#define ADDRRANGE 408
#define UNITTYPE_BYTE 409
#define UNITTYPE_KBYTES 410
#define UNITTYPE_MBYTES 411
#define UNITTYPE_TBYTES 412
#define UNITTYPE_SEC 413
#define UNITTYPE_MIN 414
#define UNITTYPE_HOUR 415
#define EOS 416
#define BOC 417
#define EOC 418
#define COMMA 419




#if ! defined YYSTYPE && ! defined YYSTYPE_IS_DECLARED
typedef union YYSTYPE
#line 174 "cfparse.y"
{
	unsigned long num;
	vchar_t *val;
	struct remoteconf *rmconf;
	struct sockaddr *saddr;
	struct sainfoalg *alg;
}
/* Line 1489 of yacc.c.  */
#line 385 "cfparse.h"
	YYSTYPE;
# define yystype YYSTYPE /* obsolescent; will be withdrawn */
# define YYSTYPE_IS_DECLARED 1
# define YYSTYPE_IS_TRIVIAL 1
#endif

extern YYSTYPE yylval;

