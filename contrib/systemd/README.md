# Introduction

This is a starter systemd unit and associated install instructions, to save new users the time of building up everything
from scratch.

This was created and works as intended on Ubuntu 22.04 LTS (Jammy Jellyfish)

# Motivations

- Save researching time for new users
- Adding common starting point for deployment files for distributions, if, as and when kafdrop gets included into
  packaging systems by linux distributions further downstream.

*Note: I could also contribute packaging information for rpm and deb formats, if that's something that'll be useful.*

# Installation steps

***Note: The assumed path of installation ($INSTALLDIR) is `/opt/kafdrop`. If any other path is used, then the service
file needs to be modified to use that path in the `# Paths` commented section.***

1. Create the directory `$INSTALLDIR` and download the latest release there.

2. Create the kafdrop user and group:
```
systemd-sysusers --inline 'u  kafdrop  -  "KafDrop user"  $INSTALLDIR  /usr/sbin/nologin'
```

3. Copy `start.sh` to `$INSTALLDIR`, edit it to fix the startup parameters and options for your use case, and give it
   execute permissions
```
chmod 755 $INSTALLDIR/start.sh
```

4. Copy the `kafdrop.service` file to `$INSTALLDIR` and create a link to it in `/etc/systemd/system`
```
(cd /etc/systemd/system && ln -s $INSTALLDIR/kafdrop.service)
```

5. Refresh systemd runtime configuration.
```
systemctl daemon-reload
```

6. Enable and start service.
```
systemctl enable kafdrop.service  && systemctl start kafdrop.service
```

7. Profit!!
