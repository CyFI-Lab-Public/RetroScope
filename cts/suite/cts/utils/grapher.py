#!/usr/bin/env python
#
# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os, sys
import get_csv_report as psr
import matplotlib.pyplot as plt
import matplotlib.mlab as mlab
import matplotlib.cbook as cbook
import matplotlib.ticker as ticker
"""
A simple script to render the data from the benchmark as a graph.
This uses MatPlotLib (http://matplotlib.org/) to plot which can be installed on linux with;
  sudo apt-get install python-matplotlib
"""

colors = {
  'maguro':'#FF0000',
  'mako':'#00FF00',
  'manta':'#0000FF',
  'tilapia':'#00FFFF'
}

def main(argv):
  if len(argv) != 2:
    print "grapher.py cts_report_dir"
    sys.exit(1)

  (_, tests) = psr.parseReports(os.path.abspath(argv[1]))

  # For each of the benchmarks
  for benchmark in tests:
    if benchmark.startswith('com.android.cts.opengl.primitive'):
      results = tests[benchmark]
      legend = []
      # Create a new figure
      fig = plt.figure()
      # Set the title of the graph
      plt.title(benchmark[benchmark.index('#') + 1:])
      # For each result in the data set
      for r in results:
        score = r.get('result', 'no results')
        x = []
        y = []
        if score == 'pass':
          y = r['details']['Fps Values']
          x = range(1, len(y) + 1)
          # Get the score, then trim it to 2 decimal places
          score = r['summary']['Average Frames Per Second']
          score = score[0:score.index('.') + 3]
        if score != 'no results':
          # Create a plot
          ax = fig.add_subplot(111)
          name = r['device']
          lbl = name + ' (%s)'%score
          clr = colors.get(name, "#%06X" % (hash(name) % 0xFFFFFF))
          # Plot the workload vs the values
          ax.plot(x, y, 'o-', label=lbl, color=clr)
          # Add a legend
          ax.legend(loc='upper right').get_frame().set_fill(False)
      (ymin, ymax) = plt.ylim()
      if ymax < 90:# So that on screen tests are easier to compare
        plt.ylim(0, 90)
      plt.xlabel('Iteration')
      plt.ylabel('FPS')
      fig.autofmt_xdate()
  # Show the plots
  plt.show()

if __name__ == '__main__':
  main(sys.argv)
