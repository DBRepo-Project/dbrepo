---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://handle.test.datacite.org/10.82556/g2ac-vh88){ .md-button .md-button--primary target="_blank" }
[:simple-jupyter: &nbsp;Notebook](https://gitlab.tuwien.ac.at/martin.weise/tres/-/blob/master/analysis.ipynb){ .md-button .md-button--secondary target="_blank" }

## Description

As part of a literature study, the research unit of data science has collected data on 47 Trusted Research Environments
(TREs) who enable analysis of confidential data under strict security assertions who protect the data with technical, 
organizational and legal measures from (accidentally) being leaked outside the facility. The literature study shows that
47 TREs worldwide provide access to confidential data of which two-thirds provide data themselves (n=32, 68%), 
predominantly via secure remote access (n=46, 98%).

## Solution

We designed a database schema that allows collection of the data with correct primary key and foreign-key relationships.
Three defined views allow for a simpler exploration of the study data. The analysis of the data was performed in TU
Wien's virtual research environment using [JupyterHub](https://science.datalab.tuwien.ac.at/) as well as the chart

<figure markdown>
![Jupyter Notebook](/infrastructures/dbrepo/1.12/images/screenshots/tre-notebook.png){ .img-border }
<figcaption>Figure 1: Jupyter Notebook accessing data on DBRepo using the Python Library.</figcaption>
</figure>

## DBRepo Features

- [x] System versioning
- [x] Subset exploration
- [x] Aggregated views
- [x] Precise & PID of queries tables
- [x] External data access for analysis

## Acknowledgement

This work was part of a cooperation with the [Research Unit of Data Science](https://informatics.tuwien.ac.at/orgs/e194-04).

<img src="/infrastructures/dbrepo/1.12/images/logos/ds-ifs.png" width=100 />