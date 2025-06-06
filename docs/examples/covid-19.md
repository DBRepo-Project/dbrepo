---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://handle.test.datacite.org/10.82556/kvsg-a919){ .md-button .md-button--primary target="_blank" }
[:simple-github: &nbsp;Archive](https://github.com/CSSEGISandData/COVID-19){ .md-button .md-button--secondary target="_blank" }

## Description

This dataset contains the daily COVID-19 data provided publicly 
by [Johns Hopkins University](https://coronavirus.jhu.edu/about/how-to-use-our-data). 

## Solution

We imported their daily snapshots provided as 1145 versioned .csv files from their Git repository archive and imported
them daily into DBRepo as system-versioned data that can be queried. During the time of this project the COVID-19 
pandemic was still ongoing and therefore daily snapshots demanded a correct import script to be maintained.

## DBRepo Features

- [x] Data pipeline from Git repository
- [x] System versioning
- [x] Subset exploration
