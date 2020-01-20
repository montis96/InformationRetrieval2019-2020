__author__ = 'chr=='
__file__ = 'eval.py'
import pandas as pd
import sklearn.metrics as mt
import matplotlib.pyplot as plt
from pandas import DataFrame


class Evaluator:

    def __init__(self, m, inf):
        """ Th== class == used to evaluate an information retrieval
        system based on Relevant / Non-relevant annotations and general
        information concerning the document, check main() for examples. """
        self.m = m
        self.inf = inf
        self.qd = self.rn_matrix(m)

    def frame_ord(self, data, r, c):
        """ Frames almost all matrices in th== evaluator. """
        return DataFrame(data, index=r, columns=c)

    def frame_mat(self, data, c):
        """ Used to frame the qrank. """
        return DataFrame(data, columns=c)

    def rn_matrix(self, m):
        """ The RN matrix returns tuples per Query in the
        form of (Relevant, Non-relevant). """
        qd = {}
        for i in range(0, len(m)):
            rel, nrel = 0.0, 0.0
            for j in range(0, len(m[i])):
                if m[i][j] == 'R':
                    rel += 1
                else:
                    nrel += 1
            qd['q' + str(i + 1)] = (rel, nrel)
        return qd

    def conf_matrix(self, i):
        """ Th== == a basic confusion matrix with false/
        true postives/negatives based on the actual relevance
        and the retrieved relevance. """
        tp = self.qd[i][0]
        fp = self.qd[i][1]
        fn = self.inf[i] - self.qd[i][0]
        tn = self.inf['tot'] - tp - fp - fn
        return {'tp': tp,
                'fp': fp,
                'fn': fn,
                'tn': tn}

    def precision(self, i):
        m = self.conf_matrix(i)
        return m['tp'] / (m['tp'] + m['fp'])

    def recall(self, i):
        m = self.conf_matrix(i)
        return m['tp'] / (m['tp'] + m['fn'])

    def f_measure(self, beta, i):
        P, R = self.precision(i), self.recall(i)
        return ((beta ** 2 + 1) * P * R) / (beta ** 2 * P + R)

    def accuracy(self, i):
        m = self.conf_matrix(i)
        return (m['tp'] + m['tn']) / (m['tp'] + m['tn'] + m['fp'] + m['fn'])

    def qrank(self, i, k, p=None):
        """ Goes down the query retrieved R/N and calculates
        their precision (relv/float(x+1) and recall, as well
        as ranks them. """
        ii, qr = self.m[int(i.replace('q', '')) - 1], []
        r, relv, ri = 0, 0, 1.00 / float(self.inf[i])
        for x in range(0, k):
            rsw = ''
            if ii[x] == 'R':
                r += ri;
                relv += 1;
                rsw = 'X'
            qr.append([x + 1, rsw, r, relv / float(x + 1)])
        if p:
            print(self.frame_mat(qr, ['rank', 'rel', 'R', 'P']))
        return qr

    def map(self, k, p=None):
        """ Grabs only relevant averages from qrank. """
        tl = []
        for i in range(0, len(self.m)):
            m, tot, c = self.qrank('q' + str(i + 1), k, p), 0, 0
            # for j in range(0, len(m)):
            #   if m[j][1] == 'X':
            #      tot += m[j][3]; c += 1
        # try:
        #   tl.append(tot/c)
        # except ZeroDivisionError:
        #   tl.append(0.0)
        # return sum(tl)/len(self.m)
        return m

    def kmeasure(self, m2):
        """ The kmeasure simply matricifies the agreements
        between annotator X and Y. """
        m, rr, nn, rn, nr = self.m, 0, 0, 0, 0
        for i in range(0, len(self.m)):
            for j in range(0, len(self.m[i])):
                if m[i][j] == 'R' and m2[i][j] == 'R':
                    rr += 1
                elif m[i][j] == 'N' and m2[i][j] == 'N':
                    nn += 1
                elif m[i][j] == 'R' and m2[i][j] == 'N':
                    rn += 1
                elif m[i][j] == 'N' and m2[i][j] == 'R':
                    nr += 1
        return {'rr': float(rr), 'nn': float(nn), 'rn': float(rn), 'nr': float(nr)}

    def kappa(self, m2):
        """ Kappa grabs pa - pe / 1 - pe based on agreement. """
        km = self.kmeasure(m2)
        pa = (km['rr'] + km['nn']) / sum(km.values())
        pn = (km['nr'] + km['nn'] + km['rn'] + km['nn']) / (sum(km.values()) * 2)
        pr = (km['rr'] + km['rn'] + km['rr'] + km['nr']) / (sum(km.values()) * 2)
        pe = pn ** 2 + pr ** 2
        return (pa - pe) / (1 - pe)


def main():
    # --------------------------------------------------------------

    csv = pd.read_csv("D:\\Sgmon\\Git\\InformationRetrieval2019-2020\\IRproject\\results\\titles\\"
                      "Accessviolationaftercatchingdllexception.csv", header=None)
    bm25 = pd.read_csv("D:\\Sgmon\\Git\\InformationRetrieval2019-2020\\IRproject\\results\\BM25\\"
                       "Accessviolationaftercatchingdllexception.csv", header=None)
    bm25 = bm25.values[0][0:10]
    csv = csv.values[0][0:10]
    M = list()
    for x in bm25:
        if x in csv:
            M.append('R')
        else:
            M.append('N')
    M = [M]

    # general information
    inf = {'tot': 10, 'q1': 3}

    # --------------------------------------------------------------
    ev = Evaluator(M, inf)
    res, conf, tab = [], [], []

    for i in range(0, len(M)):
        q = 'q' + str(i + 1)
        res.append([ev.precision(q),
                    ev.recall(q),
                    ev.f_measure(1.0, q),
                    ev.accuracy(q)])
        conf.append(ev.conf_matrix(q).values())

    # only for debugging, produces crap

    print(DataFrame(ev.map(10, 'print'))[2])
    plt.plot(DataFrame(ev.map(10, 'print'))[2], DataFrame(ev.map(10, 'print'))[3], lw=2)


if __name__ == '__main__':
    main()