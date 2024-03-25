import os
import sys
import datetime

sys.path.insert(0, os.path.abspath(".."))
sys.path.append(os.path.abspath("../dbrepo"))

# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = "dbrepo"
current_year = datetime.date.today().year
copyright = f'{current_year} the DBRepo Developers'
author = "Martin Weise"
release = "__APPVERSION__"

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = ["sphinx.ext.autodoc", "sphinx.ext.coverage", "sphinx.ext.napoleon"]
templates_path = ["templates"]
exclude_patterns = ["build", "Thumbs.db", ".DS_Store", "debug.py", "setup.py"]

# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = "furo"
html_title = f"DBRepo Python Library {release} documentation"
html_logo_width = 200
html_static_path = ["static"]
html_theme_options = {
    "light_logo": "theme_light_logo.png",
    "dark_logo": "theme_dark_logo.png"
}
html_css_files = [
    "css/custom.css",
]
html_output_encoding = 'utf-8'
html_show_sourcelink = False
html_sidebars = {
    "**": [
        "sidebar/brand.html",
        "sidebar/search.html",
        "sidebar/scroll-start.html",
        "sidebar/feedback.html",
        "sidebar/navigation.html",
        "sidebar/scroll-end.html",
    ]
}
