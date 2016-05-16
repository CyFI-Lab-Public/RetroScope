#!/system/bin/sh

IPTABLES="/system/bin/iptables"

#$IPTABLES -t security -A INPUT -i wlan0 -j SECMARK --selctx u:object_r:packet:s0
#$IPTABLES -t security -A INPUT -i lo -j SECMARK --selctx u:object_r:lo_packet:s0
#$IPTABLES -t security -A INPUT -i ppp0 -j SECMARK --selctx u:object_r:ppp0_packet:s0
#$IPTABLES -t security -A INPUT -i ppp1 -j SECMARK --selctx u:object_r:ppp1_packet:s0
#$IPTABLES -t security -A INPUT -i ppp2 -j SECMARK --selctx u:object_r:ppp2_packet:s0
#$IPTABLES -t security -A INPUT -i ppp3 -j SECMARK --selctx u:object_r:ppp3_packet:s0

#$IPTABLES -t security -A OUTPUT -o wlan0 -j SECMARK --selctx u:object_r:packet:s0
#$IPTABLES -t security -A OUTPUT -o lo -j SECMARK --selctx u:object_r:lo_packet:s0
#$IPTABLES -t security -A OUTPUT -o ppp0 -j SECMARK --selctx u:object_r:ppp0_packet:s0
#$IPTABLES -t security -A OUTPUT -o ppp1 -j SECMARK --selctx u:object_r:ppp1_packet:s0
#$IPTABLES -t security -A OUTPUT -o ppp2 -j SECMARK --selctx u:object_r:ppp2_packet:s0
#$IPTABLES -t security -A OUTPUT -o ppp3 -j SECMARK --selctx u:object_r:ppp3_packet:s0
