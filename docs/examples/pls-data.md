---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://handle.test.datacite.org/10.82556/9y6c-9038){ .md-button .md-button--primary target="_blank" }
[:simple-jupyter: &nbsp;Notebook](https://gitlab.tuwien.ac.at/martin.weise/pl-spectrum/-/blob/master/PL-Spectroscopy%20Sr2SnO4-Ti4+.ipynb){ .md-button .md-button--secondary target="_blank" }

## Description

Nanocrystalline luminescence material of Sr2SnO4:Ti4+ has been synthesized by sonochemical method. The obtained powder 
then received heating treatment at different temperature of 100, 400, 600, 800, 1000, and 1400&deg;C for two hours. 
X-ray diffraction pattern indicated that heating at 100&deg;C would result in the presence of Sr2Sn(OH)8 as the dominant
phase. Meanwhile, heating at 400&deg;C stabilize SrSnO3 structure. 

## Solution

We used Python to read the imported [`raw/`](https://gitlab.tuwien.ac.at/martin.weise/pl-spectrum/-/tree/master/raw)
data into DBRepo. Then we access the data from DBRepo through the Jupyter Notebook and filter the integer-multiples of
the wavelength of the light source `f=254` nm. The remainder of the work was to match the chart design using `pyplot`.

<figure markdown>
![Jupyter Notebook](/infrastructures/dbrepo/1.13/images/screenshots/pls-chart.png){ .img-border }
<figcaption>Figure 1: Reproduction of experiment chart.</figcaption>
</figure>

## DBRepo Features

- [x] Data preservation of raw PL-spectography data
- [x] Subset exploration
- [x] External visualization of the database
- [x] Replication of experiments using only open-source software

## Acknowledgement

This work was part of a cooperation with the [Institute of Technology Bandung](https://itb.ac.id/).

<img src="/infrastructures/dbrepo/1.13/images/logos/itb.png" width=100 />