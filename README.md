# " This project is tested with BrowserStack.”
 
# FIXED CONNECTION PROBLEMS AND SOME BUGS AND REMOVED UNNECESSARY CODE BLOCKS
# Just install the app and click start button and it should work properly
# YOU CAN ALSO ENABLE PROXY MODE TO ROUTE ALL YOUR TRAFFIC TO THİS SPECIFIED PROXY SERVER

# Android-VpnSocket-VpnProxy



Quick and easy VPN to Socket using packet manipulation. Create proxys and firewalls easily globally!

How can I create a proxy or firewall within this library? 
-----------
All you need to do is modify the ```VPN/Proxy.java``` file.
[Proxy.java](https://github.com/ubeyid/Android-VpnSocket-VpnProxy/blob/master/app/app/src/main/java/vpntosocket/shadowrouter/org/vpntosocket/VPN/Proxy.java)

How it works
-----------
VPNs and Sockets are on 2 different layers which makes this project a little bit difficult, however the task is not impossible. This project works by sorting packets based off of type: UDP, TCP, ICMTP. We then take all TCP packets and sort them using a NAT, this makes it easier to identify where each packet is supposed to go. Once the packets are sorted we will take the TCP packets and change the to IP address and port to a local socket so that you can do whatever you want with the socket.

