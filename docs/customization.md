---
author: Martin Weise
hide:
- navigation
---

# Customization

!!! info "Abstract"

    On this page, we showcase the customization capabilities of DBRepo to e.g. whitelabel your deployment.

## Custom Title

The default title is "Database Repository" and can be replaced by changing the `TITLE` environment variable.

## Custom Logo

The default placeholder logo consists of the two universities that developed this software. You can replace it by
mounting your own logo with a volume.

```console
docker run -v /path/to/your_logo.png:/logo.png ...
```

In case your logo is not in PNG format, you need to change the environment variable `LOGO` accordingly, e.g. for a logo
in JPEG format, set `LOGO: "/logo.JPEG"` in the `.env` file and start the container with a volume.

```console
docker run -v /path/to/your_logo.JPEG:/logo.JPEG ...
```

<figure markdown>
![](/infrastructures/dbrepo/1.12/images/custom_logo.png)
</figure>

## Custom Icon

The default placeholder icon can be replaced by mounting your own icon with a volume.

```console
docker run -v /path/to/your_logo.ico:/favicon.ico ...
```

<figure markdown>
![](/infrastructures/dbrepo/1.12/images/custom_icon.png)
</figure>

In case your icon is not in ICO format, you need to change the environment variable `ICON` accordingly, e.g. for an icon
in GIF format, set `ICON: "/favicon.GIF"` in the `.env` file and start the container with a volume.

```console
docker run -v /path/to/favicon.GIF:/favicon.GIF ...
```
