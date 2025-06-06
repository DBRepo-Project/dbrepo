---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://dbrepo1.ec.tuwien.ac.at/database/14daff7e-9dae-4ba0-9887-6ded15572c4e){ .md-button .md-button--primary target="_blank" }

## Description

The [Pilotfabrik]() of TU Wien is monitored for energy-efficiency and productivity of machinery. In principle, certain
conditions/parameters are observed such as: electric rate of energy transfer, transmission of cooling liquid,
transmission of compressed air, acceleration, forces at work and temperatures to research on preventive/predictive
maintenance, quality of products and ultimately process efficiency and -productivity.

<figure markdown>
![](../../images/screenshots/power.png)
<figcaption>Figure 1: Total power usage of machine floor TU Pilotfabrik, image from <a href="https://publik.tuwien.ac.at/files/PubDat_252294.pdf">Hacksteiner (2016)</a>.</figcaption>
</figure>

## Solution

We connected our [Broker Service](../../api/broker-service) with the MQTT broker of the Pilotfabrik using a self-written
connector service, bridging the two different protocols. The tuples are ingested into DBRepo at a rate of about 10/s.

## DBRepo Features

- [x] High-throughput real-time data import (MQTT)
- [x] Private database
- [x] Public embargoed data view

## Acknowledgement

This work was part of a cooperation with the [Institute of Production Engineering and Photonic Technologies](http://ift.at/).

<img src="../../images/logos/ift.jpeg" width=100 />
