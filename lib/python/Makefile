all: build install

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

install:
	cp ./dist/dbrepo-* ../../dbrepo-analyse-service/lib/
	(cd ../../dbrepo-analyse-service && pipenv lock)
	cp ./dist/dbrepo-* ../../dbrepo-search-service/lib/
	(cd ../../dbrepo-search-service && pipenv lock)
	cp ./dist/dbrepo-* ../../../dpm2024/lib/
	(cd ../../../dpm2024 && pipenv lock)

deploy: build
	python3 -m twine upload --config-file ~/.pypirc --verbose --repository pypi ./dist/dbrepo-*

deploy-test: build
	python3 -m twine upload --config-file ~/.pypirc --verbose --repository testpypi ./dist/dbrepo-*

FORCE: ;