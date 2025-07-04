---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://handle.test.datacite.org/10.82556/g00j-se74){ .md-button .md-button--primary target="_blank" }
[:simple-grafana: &nbsp;Dashboard](https://dbrepo1.ec.tuwien.ac.at/dashboard/d/cehxheycgsyyob){ .md-button .md-button--secondary target="_blank" }

## Description

The Subway Transportation Data-Dataset is a comprehensive and dynamic collection of data that captures the intricate 
details of the city's public transportation system. This dataset encompasses a wide array of information, including bus 
and tram schedules, subway routes, ticketing details, and real-time updates on vehicle locations.

## Solution

We wrote an algorithm that parses open data (available) information from Wiener Linien, Vienna's public transportation
agency directly and feeds it, after some cleaning, into DBRepo on a 5-minute interval.

<figure markdown>
![Subway Transportation Data Dashboard](/infrastructures/dbrepo/1.10/images/screenshots/transportation-dashboard.png)
<caption>Figure 1: Dashboard visualizing the live data of the current interruptions.</caption>
</figure>

## DBRepo Features

- [x] Dynamic data (live data)
- [x] System versioning
- [x] Subset exploration
- [x] External visualization of the database
- [x] Mix of managed and unmanaged content for dashboards