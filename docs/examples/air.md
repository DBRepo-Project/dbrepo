---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://handle.test.datacite.org/10.82556/gd17-aq82){ .md-button .md-button--primary target="_blank" }
[:simple-grafana: &nbsp;Dashboard](https://dbrepo.datalab.tuwien.ac.at/dashboard/d/aehxhey228740f){ .md-button .md-button--secondary target="_blank" }

## Description

This digital record contains historical air pollution and air quality data from approximately 20 air monitoring stations
in Vienna, spanning the years from 1980 to 2021. The data was provided by the Umweltbundesamt and is stored in its
original form in this record. This record forms the basis of an analysis carried out in a bachelor's thesis at the TU 
Wien.

<figure markdown>
![Grafana Dashboard](/infrastructures/dbrepo/1.13/images/screenshots/air-dashboard.png)
<figcaption>Figure 1: Grafana dashboard visualizing the dataset.</figcaption>
</figure>

The analysis was carried out in a Jupyter Notebook hosted by our IT-department
[JupyterHub](https://science.datalab.tuwien.ac.at/) as part of TU Wien's virtual research environment.

<figure markdown>
![Jupyter Notebook](/infrastructures/dbrepo/1.13/images/screenshots/air-notebook.png){ .img-border }
<figcaption>Figure 2: Jupyter Notebook accessing data on DBRepo using the Python Library.</figcaption>
</figure>

## Solution

One of the first use-cases of importing external data into DBRepo which was provided as .csv flat file. We developed a
database schema and a web scraper that scrapes live air quality data from the 
[public map](https://luft.umweltbundesamt.at/pub/map_chart/index.pl) of the environment agency of Austria.

## DBRepo Features

- [x] Import complex dataset
- [x] System versioning
- [x] Subset exploration
- [x] Aggregated views
- [x] Precise & PID of queries tables
- [x] External data access for analysis

## Acknowledgement

This work was part of a cooperation with the [Umweltbundesamt](https://www.umweltbundesamt.at/).

<img src="/infrastructures/dbrepo/1.13/images/logos/umweltbundesamt.png" width=100 />