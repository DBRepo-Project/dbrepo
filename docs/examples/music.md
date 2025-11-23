---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://handle.test.datacite.org/10.82556/zxww-q738){ .md-button .md-button--primary target="_blank" }
[:material-file-document: &nbsp;Archive](https://gitlab.tuwien.ac.at/martin.weise/fairnb){ .md-button .md-button--secondary target="_blank" }

## Description

We use a dataset collected by [Aljanaki et al.](https://www2.projects.science.uu.nl/memotion/emotifydata/), consisting 
of 400 MP3 music files, each having a playtime of one minute and labeled with one of four genres: rock, pop, classical 
and electronic, each genre contains 100 files, the genre will be used as label for the ML model. Then by generating MFCC
vectors and training a SVM, the ML-model can classify emotions of the provided .mp3 files with and accuracy of 76.25%.

<figure markdown>
![](/infrastructures/dbrepo/1.13/images/screenshots/mfcc-jupyter.png){ .img-border }
<figcaption>Figure 1: Accuracy of predictions matrix in Jupyter Notebook.</figcaption>
</figure>

## Solution

DBRepo is used as relational data storage of the raw- and aggregated features, prediction results and the splits of the
training- and test data. For each of the 400 .mp3 files, 40 MFCC feature vectors are generated. This data is stored
in aggregated form in the [`aggregated_features`](https://dbrepo1.ec.tuwien.ac.at/pid/47) table.

## DBRepo Features

- [x] Database as storage for machine learning data
- [x] System versioning
- [x] Subset exploration
- [x] Precise & PID of database tables
- [x] External data access for analysis
