import argparse
import requests
import matplotlib.pyplot as plt
import csv
import numpy as np
import base64

war_endpoint = "http://localhost:8000/insectwar?"

for max in range(1, 100, 10):
    for army1 in range(1, 50, 10):
        for army2 in range(1, 50, 10):

            endpoint = war_endpoint + "max={}&army1={}&army2={}".format(max,army1,army2)
            requests.get(endpoint)


# draw a graph using plotlib of the results assuming the results are in a csv file
with open('insectwar.csv', 'r') as csvfile:
    plots = csv.reader(csvfile, delimiter=',')
    for row in plots:
        plt.plot(row[0], row[1])
        
    