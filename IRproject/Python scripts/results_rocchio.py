import pandas as pd
import os
import statistics
import numpy

all_files = os.listdir("D:\\Sgmon\\Git\\InformationRetrieval2019-2020\\IRproject\\results\\titles")
M = []
n_relevant = []
n_retrieved = []
for file in all_files:
    csv = pd.read_csv("D:\\Sgmon\\Git\\InformationRetrieval2019-2020\\IRproject\\results\\titles\\" +
                      file, header=None)
    bm25 = pd.read_csv("D:\\Sgmon\\Git\\InformationRetrieval2019-2020\\IRproject\\results\\Rocchio\\" +
                   file, header=None)
    bm25 = bm25.values[0][0:-1]
    csv = csv.values[0][0:-1]
    n_relevant.append(len(csv))
    n_retrieved.append(len(bm25))
    row = list()
    for x in bm25:
        if x in csv:
            row.append(1)
        else:
            row.append(0)
    M.append(row)


relevant_retrieved = [sum(x) for x in M]
recall = []
precision = []
for x in range(0, len(relevant_retrieved)):
    precision.append(relevant_retrieved[x]/n_retrieved[x])
    recall.append(relevant_retrieved[x]/n_relevant[x])
print("Precision average: ", sum(precision)/len(precision))
print("Precision sd: ", statistics.stdev(precision))
print("Recall: ", sum(recall)/len(recall))
print("recall sd: ", statistics.stdev(recall))





