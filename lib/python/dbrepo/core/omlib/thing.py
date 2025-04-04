from rdflib import BNode, URIRef, Literal, XSD


class Thing:

    def __init__(self, label=None, identifier=None):
        if identifier is None:
            self.identifier = BNode()
        else:
            self.identifier = URIRef(identifier)
        if label is None:
            self.prefLabels = []
        else:
            self.prefLabels = []
            self.__add_label_to_array(self.prefLabels, label, None)
        self.altLabels = []

    def __add_label_to_array(self, array, label, language=None):
        if isinstance(label, list):
            for item in label:
                self.__add_label_to_array(array, item, language)
        else:
            if language is None:
                if isinstance(label, Literal) and not label in array:
                    array.append(label)
                else:
                    label_lit = Literal(label, datatype=XSD.string)
                    if not label_lit in array:
                        array.append(label_lit)
            else:
                if isinstance(label, Literal) and not label in array:
                    label_lit = Literal(label.normalize, language)
                    array.append(label_lit)
                else:
                    label_lit = Literal(label, language)
                    if not label_lit in array:
                        array.append(label_lit)

    def add_preferred_label(self, label, language=None):
        # TODO Check if a preferred label in the specified language already exists, if so move the old one to alt labels
        self.__add_label_to_array(self.prefLabels, label, language)

    def add_alternative_label(self, label, language=None):
        self.__add_label_to_array(self.altLabels, label, language)

    def label(self):
        label = self.preferred_label()
        return label

    def preferred_label(self, language=None):
        result_label = None
        if language is None:
            for pref_label in self.prefLabels:
                if pref_label.language is None:
                    result_label = pref_label
            if result_label is None:
                for pref_label in self.prefLabels:
                    if pref_label.language == 'en':
                        result_label = pref_label
        else:
            for pref_label in self.prefLabels:
                if pref_label.language == language:
                    result_label = pref_label
        return result_label

    def all_labels(self):
        labels = []
        labels.extend(self.prefLabels)
        labels.extend(self.altLabels)
        return labels


class SymbolThing(Thing):

    def __init__(self, name=None, symbol=None, identifier=None):
        super().__init__(name, identifier)
        self.dimensions = []
        self.symbols = []
        self.add_symbol(symbol)

    def add_symbol(self, symbol, language=None):
        if isinstance(symbol, list):
            for item in symbol:
                self.__add_label_to_array(self.symbols, item, language)
        else:
            if language is None:
                if isinstance(symbol, Literal) and not symbol in self.symbols:
                    self.symbols.append(symbol)
                else:
                    label_lit = Literal(symbol, datatype=XSD.string)
                    if not label_lit in self.symbols:
                        self.symbols.append(label_lit)
            else:
                if isinstance(symbol, Literal) and not symbol in self.symbols:
                    label_lit = Literal(symbol.normalize, language)
                    self.symbols.append(label_lit)
                else:
                    label_lit = Literal(symbol, language)
                    if not label_lit in self.symbols:
                        self.symbols.append(label_lit)

    def symbol(self):
        symbol = self.preferred_symbol()
        return symbol

    def preferred_symbol(self, language=None):
        result_symbol = None
        if language is None:
            for symbol in self.symbols:
                if symbol.language is None:
                    result_symbol = symbol
            if result_symbol is None:
                for symbol in self.symbols:
                    if symbol.language == 'en':
                        result_symbol = symbol
        else:
            for symbol in self.symbols:
                if symbol.language == language:
                    result_symbol = symbol
        return result_symbol

    def all_symbols(self):
        return self.symbols
