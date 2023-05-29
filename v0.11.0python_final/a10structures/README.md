# A10 Libraries

These libraries provide the core functionality for the Nokia Attestation Engine.

The instructions here are to be used while developing and testing. To run in a production/sane environment use docker-compose.

## Build and Install Locally
Run the commands below together as follows. There's also a command included to remove pyc files and the pycache folder just in case because these have a nasty habit of hanging around and causing really odd bugs such as missing attributes and modules even though the files are really there! 

```bash
find . | grep -E "(__pycache__|\.pyc|\.pyo$)" | xargs rm -rf
sudo pip3 uninstall a10
sudo python3 setup.py sdist bdist_wheel
sudo python3 setup.py install
```


## Upload to Artifactory

Edit `~/.pypirc' as follows:

```
[distutils]
index-servers = local
[local]
repository: https://artifactory-espoo2.int.net.nokia.com/artifactory/api/pypi/nbl-nae-pypi-local
username: ioliver
password: (see artifactor SETUP page for the hashed password)
```

Then

```bash
sudo -E python3 setup.py sdist bdist_wheel upload -r local
```


## Documentation

Generated using sphinx

To generate the API documents:

```bash
cd doc
sphinx-apidoc -f -o source/ ../a10/
make html
```

NB: this leaves stuff around which sphinx-build picks up on, eg: old files etc. Will have to find out how to properly clean this directory. Using `make clean` didn't seem to help.

### Some Sphinx stuff for reference
For reference the conf.py file contains the lines

```python
import os
import sys
# sys.path.insert(0, os.path.abspath('.'))
sys.path.insert(0, os.path.abspath('../../a10'))
print("SPHINX PATH=",sys.path)
```

This worked on Sphinx 1.8.5 .. the `needs_sphinx` entry should be updated to reflect this.

The extensions required are:

```python
extensions = [
    'sphinx.ext.autodoc',
    'sphinx.ext.todo',
]
```

If the RTD theme is used:

```bash
pip3 install sphinx-rtd-theme
```

The entry in the conf.py is

```
html_theme = "sphinx_rtd_theme"
```


