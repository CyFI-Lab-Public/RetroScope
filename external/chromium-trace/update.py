#!/usr/bin/python2.6

import httplib, json, optparse, os, urllib, shutil, subprocess, sys

output_css_file = 'style.css'
output_js_file = 'script.js'

upstream_svn = 'http://trace-viewer.googlecode.com/svn/trunk/'

script_dir = os.path.dirname(os.path.abspath(sys.argv[0]))
trace_viewer_dir = os.path.join(script_dir, 'trace-viewer')

parser = optparse.OptionParser()
parser.add_option('--local', dest='local_dir', metavar='DIR',
                  help='use a local trace-viewer')
parser.add_option('--no-min', dest='no_min', default=False, action='store_true',
                  help='skip minification')
options, args = parser.parse_args()

if options.local_dir is None:
  # Remove the old source
  shutil.rmtree(trace_viewer_dir, True)

  # Pull the latest source from the upstream svn
  svn_co_args = ['svn', 'co', upstream_svn, trace_viewer_dir]
  p = subprocess.Popen(svn_co_args, stdout=subprocess.PIPE)
  svn_output = ''
  while p.poll() is None:
    svn_output += p.stdout.read()
  if p.returncode != 0:
    print 'Failed to checkout source from upstream svn.'
    sys.exit(1)

  # Update the UPSTREAM_REVISION file
  rev_str = svn_output.split('\n')[-2]
  if not rev_str.startswith('Checked out revision '):
    print 'Unrecognized revision string: %q' % rev_str
  open('UPSTREAM_REVISION', 'wt').write(rev_str[21:-1] + '\n')
else:
  trace_viewer_dir = options.local_dir

# Generate the flattened JS and CSS
build_dir = os.path.join(trace_viewer_dir, 'build')
sys.path.append(build_dir)
gen = __import__('generate_standalone_timeline_view', {}, {})
js_code = gen.generate_js()
css_code = gen.generate_css()

if options.no_min:
  open(output_js_file, 'wt').write(js_code)
  print 'Generated %s' % output_js_file
  open(output_css_file, 'wt').write(css_code)
  print 'Generated %s' % output_css_file
else:
  # Define the parameters for the POST request and encode them in
  # a URL-safe format.
  params = urllib.urlencode([
    ('js_code', js_code),
    ('language', 'ECMASCRIPT5'),
    ('compilation_level', 'SIMPLE_OPTIMIZATIONS'),
    ('output_format', 'json'),
    ('output_info', 'errors'),
    ('output_info', 'compiled_code'),
  ])

  # Always use the following value for the Content-type header.
  headers = { "Content-type": "application/x-www-form-urlencoded" }
  conn = httplib.HTTPConnection('closure-compiler.appspot.com')
  conn.request('POST', '/compile', params, headers)
  response = conn.getresponse()
  data = response.read()
  conn.close

  if response.status != 200:
    print sys.stderr, "error returned from JS compile service: %d" % response.status
    sys.exit(1)

  result = json.loads(data)
  if 'errors' in result:
    print 'Encountered error minifying Javascript.  Writing intermediate code to flat_script.js'
    open('flat_script.js', 'wt').write(js_code)
    for e in result['errors']:
      filenum = int(e['file'][6:])
      filename = 'flat_script.js'
      lineno = e['lineno']
      charno = e['charno']
      err = e['error']
      print '%s:%d:%d: %s' % (filename, lineno, charno, err)
    print 'Failed to generate %s.' % output_js_file
    sys.exit(1)

  open(output_js_file, 'wt').write(result['compiledCode'] + '\n')
  print 'Generated %s' % output_js_file

  yuic_args = ['yui-compressor', '--type', 'css', '-o', output_css_file]
  p = subprocess.Popen(yuic_args, stdin=subprocess.PIPE)
  p.communicate(input=css_code)
  if p.wait() != 0:
    print 'Failed to generate %s.' % output_css_file
    sys.exit(1)

  print 'Generated %s' % output_css_file
