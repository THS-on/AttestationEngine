# Trust Agent

This is the code for the *reference* trust agent. It basically supports EVERY API I can think of; inlcuding things that are not supported on some devices, eg: UEFI on ARM.


## What's here

   * In the `py` directory is the Python3 POC TA.   <--- don't use this
   * In the `nut10` directory is the better Python3 POC TA   <--- USE THIS   
   * In the `go` directory is the GoLang POC TA.      <--- incomplete
   * In the `systemd` directory are the templates for the systemd services and the start and stop scripts
   

 
## Installation

### Podman (and Docker)

In the nut10 directory there is a `Dockerfile` which can be used. Build and running commands below (select which ones you need - don't try to run all of them)

If you're running against a software TPM, eg: IBM's swtpm where the TCTI is set to mssim, then I'm not sure how to run this - probably use bare metal instead.

```bash
cd nut10
podman build -t nut10 .
docker run -p 8530:8530 --device=/dev/tpm0 nut10
podman run -p 8530:8530 --device=/dev/tpm0 nut10
```


### Bare Metal

Easier method is to download the code , eg: git pull, and then do this - it installed the python version. I am assuming you are in the t10 directory.

Before starting this, it might be easier to manually create `/opt/ta` and change its owner to someone who can easily write, eg: the current user.  Some comments require sudo.

Also, edit ta.serivce and check the requiremnts on the Wants sections for abrmd machines and PiFakeBoot machines.

You will also need to check the port number in `ta_config.cfg` - this should be 8530. This file is used by python flask.

Finally create `/etc/t10.conf`. There is an example `t10.conf` file in the nut10 directory. The main thing you need to do is provide a comma separated list of attestation servers that the t10 will contact on startup via the a10rest API. For example:

```bash
[Asvr]
    Asvrs=http://127.0.0.1:8520,http://127.0.0.1:8520
```

Typically there is only one server, but you may have more:

Once you've put t10.conf in /etc/ you can install the nut10 trust agent like so:

```bash
systemctl stop ta.service
mkdir /opt/ta
cp -r nut10/* /opt/ta
cp systemd/ta.service /etc/systemd/system
cp systemd/ta.start /opt/ta
cp systemd/ta.stop /opt/ta
chmod 644 /opt/ta/ta.start
chmod 644 /opt/ta/ta.stop
systemd daemon-reload
sudo systemd enable ta.service
systemd start ta.service
```
 
 
   
## A Note on Raspberry PIs

Good to know, the TA basically starts in more or less the same way. However, the Let's Trust TPMs can perform a proper reset as if they were power cycled using the GPIO pins, where as the Infineon TPMs require someone to press the reset button on the device (unless you rewire the big round gold wire).

Actually the above probably should be in the Fake measured boot for the Pis.
