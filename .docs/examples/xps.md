---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://dbrepo1.ec.tuwien.ac.at/database/27/info){ .md-button .md-button--primary target="_blank" }
[:simple-jupyter: &nbsp;Notebook](https://binder.science.datalab.tuwien.ac.at/v2/git/https%3A%2F%2Fgitlab.tuwien.ac.at%2Ffairdata%2Fxps/HEAD){ .md-button .md-button--secondary target="_blank" }

## Description

X-ray Photoelectron Spectroscopy (XPS) is one of the most used methods in material sciences. Irradiation of solid
materials with X-ray radiation kicks out electrons from atoms that are near the atomic nucleus. With XPS data being
highly reproducible once machine parameters are known and understood, the demand for creating a comprehensive database
connecting material properties to compositions via XPS spectra becomes evident.

## Solution

We read XPS data from the VAMAS-encoded format and inserted it into a 
[database schema](https://gitlab.tuwien.ac.at/fairdata/xps/-/blob/e17860399b1b109c72b01888766f37193dde5870/sql/create_schema.sql) 
that captures the VAMAS-schema. It can then be read using the [Python Library](../../api/python).

<figure markdown>
![Jupyter Notebook](../../images/screenshots/xps-jupyter.png){ .img-border }
<figcaption>Figure 1: Jupyter Notebook accessing data on DBRepo using the Python Library.</figcaption>
</figure>

Using the DataFrame representation of the Python Library and the [`plotly`](https://pypi.org/project/plotly/) library,
we can visualize the ordinate values directly in the Jupyter Notebook.

<figure markdown>
![Three charts displaying surface analysis data of C, O and Su](../../images/screenshots/xps-chart.png){ .img-border }
<figcaption>Figure 2: Plot of ordinate values encoded within the experiment block.</figcaption>
</figure>

## DBRepo Features

- [x] Data preservation of VAMAS-encoded XPS data
- [x] Subset exploration
- [x] External visualization of the database
- [x] Replication of experiments using only open-source software

## Acknowledgement

This work was part of a cooperation with the [Institute of Applied Physics](http://www.iap.tuwien.ac.at/).

<img src="../../images/logos/iap.jpeg" width=100 />