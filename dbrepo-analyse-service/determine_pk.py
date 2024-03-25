import json
import logging
import pandas as pd
import random
import numpy as np
import math
from determine_dt import determine_datatypes
from clients.s3_client import S3Client


def determine_pk(filename, separator=","):
    dt = json.loads(determine_datatypes(filename=filename, separator=separator))
    dt = {k.lower(): v for k, v in dt["columns"].items()}
    # {k.lower(): v for k, v in dt['columns'].items() if v != 'Numeric'}
    colnames = dt.keys()
    colindex = list(range(0, len(colnames)))

    s3_client = S3Client()
    s3_client.file_exists('dbrepo-upload', filename)
    response = s3_client.get_file('dbrepo-upload', filename)
    stream = response['Body']
    if response['ContentLength'] == 0:
        raise OSError(f'Failed to determine primary key: file {filename} has empty body')
    sizeInKb = math.ceil(response['ContentLength'] / 1000)
    if sizeInKb < 400:  # precise if lower than 400kB
        pk = {}
        j = 0
        k = 0
        logging.info(f"File is {sizeInKb}kb, detection is precise")
        for item in colnames:
            if item == "id":
                j = j + 1
                pk.update({item: j})
                colindex.remove(k)
            k = k + 1
        csvdata = pd.read_csv(stream, sep=separator)
        for i in colindex:
            if pd.Series(csvdata.iloc[:, i]).is_unique and pd.Series(csvdata.iloc[:, i]).notnull().values.any():
                j = j + 1
                pk.update({list(colnames)[i]: j})
    else:  # stochastic pk determination
        pk = {}
        j = 0
        k = 0
        logging.info(
            f"File is {sizeInKb}kB (larger than threshold of 400kB), detection is stochastic"
        )
        for item in colnames:
            if item == "id":
                j = j + 1
                pk.update({item: j})
                colindex.remove(k)
            k = k + 1
        p = np.log10(
            int(response["ContentLength"])
        )  # logarithmic scaled percentage of random inspected rows
        csvdata = pd.read_csv(
            filepath_or_buffer=stream,
            sep=separator,
            header=0,
            skiprows=lambda k: k > 0 and random.random() > p,
        )
        for i in colindex:
            if pd.Series(csvdata.iloc[:, i]).is_unique and pd.Series(csvdata.iloc[:, i]).notnull().values.any():
                j = j + 1
                pk.update({list(colnames)[i]: j})
        logging.info(f"Determined primary key {pk}")
    return pk
