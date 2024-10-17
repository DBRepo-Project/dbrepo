all: build

clean:
	rm -rf ./python/dist/* ./docs/build/* ./dist/*

docs: clean
	sphinx-apidoc -o ./docs/source ./dbrepo
	sphinx-build -M html ./docs/ ./docs/build/

check:
	python3 ./python/setup.py develop

build: clean
	python3 -m build --sdist .
	python3 -m build --wheel .

deploy: build
	python3 -m twine upload --config-file ~/.pypirc --verbose --repository pypi ./dist/dbrepo-*

deploy-test: build
	python3 -m twine upload --config-file ~/.pypirc --verbose --repository testpypi ./dist/dbrepo-*

FORCE: ;