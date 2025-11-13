---
author: Martin Weise
---

!!! danger "Security Disclaimer"

    This quick default installation should **not be considered secure**. It is intended for local testing and
    demonstration and should not be used in public deployments or in production. It is a quick installation method and
    is intended for a quick look at DBRepo.

## TL;DR

[Download the live image](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.12.0/debian-13-testing-live-dbrepo-amd64.iso) and create a VM using your hypervisor. Below is an [extended example](#example) on how to do this on Debian with libvirt.

Then start DBRepo and visit `http://<hostname_or_ip>` in your browser:

```shell
docker compose up -d
```

## Example

In this extended example we setup the live image using `libvirt` and `virt-manager` via QEMU. First, install the dependencies:


=== "Debian"

    ```shell
    apt install virt-manager
    ```

Next, check your system compatibility:

=== "Debian"

    ```shell
    virt-host-validate qemu
    ```

The result should at least `PASS` for `/dev/kvm`.

Open the `virt-manager` application and choose "Network install":

```shell
virt-manager \
  --connect "qemu:///system" \
  --show-domain-creator
```

* Fill in the operating system install URL `https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.12.0/debian-13-testing-live-dbrepo-amd64.iso`
* Choose the operating system `Debian 13` (or similar)
* Assign at least 4096 MiB memory and 4 CPUs
* Assign at least 25 GiB virtual disk storage

When the virtual machine is started, continue by starting DBRepo:

```shell
docker compose up -d
```

And then find out the hostname or IP address of the VM:

```shell
hostname -I | cut -d' ' -f1
```

This hostname or IP address is then used on your local machine (e.g. laptop) to access the DBRepo instance: `http://<hostname_or_ip>`
