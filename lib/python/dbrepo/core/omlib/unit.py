import math

from rdflib import URIRef

from dbrepo.core.omlib.exceptions.dimensionexception import DimensionalException
from dbrepo.core.omlib.exceptions.unitconversionexception import UnitConversionException
from dbrepo.core.omlib.exceptions.unitidentityexception import UnitIdentityException
from dbrepo.core.omlib.dimension import Dimension
from dbrepo.core.omlib.thing import SymbolThing


class Unit(SymbolThing):
    _units = []

    @staticmethod
    def with_label(label):
        get_units = []
        for unit in Unit._units:
            for u_label in unit.all_labels():
                if str(u_label) == label:
                    get_units.append(unit)
                    break
        return get_units

    @staticmethod
    def with_symbol(symbol):
        get_units = []
        for unit in Unit._units:
            for u_symbol in unit.all_symbols():
                if str(u_symbol) == symbol:
                    get_units.append(unit)
        return get_units

    @staticmethod
    def with_identifier(identifier):
        for unit in Unit._units:
            if str(unit.identifier) == str(identifier):
                return unit
        return None

    @staticmethod
    def with_prefix(prefix, base):
        test_units = Unit.with_dimensions(base.dimensions)
        for unit in test_units:
            if isinstance(unit, PrefixedUnit):
                if unit.baseUnit == base and unit.prefix == prefix:
                    return unit
        return None

    @staticmethod
    def with_multiplication(multiplier, multiplicand):
        new_dimensions = multiplier.dimensions * multiplicand.dimensions
        test_units = Unit.with_dimensions(new_dimensions)
        for unit in test_units:
            if isinstance(unit, UnitMultiplication):
                if unit.multiplier == multiplier and unit.multiplicand == multiplicand:
                    return unit
        return None

    @staticmethod
    def with_division(numerator, denominator):
        new_dimensions = numerator.dimensions / denominator.dimensions
        test_units = Unit.with_dimensions(new_dimensions)
        for unit in test_units:
            if isinstance(unit, UnitDivision):
                if unit.numerator == numerator and unit.denominator == denominator:
                    return unit
        return None

    @staticmethod
    def with_exponentiation(base, exponent):
        new_dimensions = Dimension.pow(base.dimensions, exponent)
        test_units = Unit.with_dimensions(new_dimensions)
        for unit in test_units:
            if isinstance(unit, UnitExponentiation):
                if unit.base == base and unit.exponent == exponent:
                    return unit
        return None

    @staticmethod
    def with_dimensions(dimensions, in_system_of_units=None):
        if isinstance(dimensions, Dimension):
            get_units = []
            for test_unit in Unit._units:
                if test_unit.dimensions == dimensions:
                    if in_system_of_units is None or test_unit.systemOfUnits == in_system_of_units:
                        get_units.append(test_unit)
            return get_units
        return None

    @staticmethod
    def get_base_units(for_unit, in_system_of_units=str('http://www.ontology-of-units-of-measure.org/resource/om-2/InternationalSystemOfUnits')):  # WARNING If you change "SI" also change in constants.py
        if isinstance(for_unit, SingularUnit) or isinstance(for_unit, PrefixedUnit) \
                or isinstance(for_unit, UnitMultiple):
            units = Unit.with_dimensions(for_unit.dimensions)
            for unit in units:
                if unit.isBaseUnit and unit.systemOfUnits == in_system_of_units:
                    return unit
        if isinstance(for_unit, UnitMultiplication):
            base_multiplier = Unit.get_base_units(for_unit.multiplier, in_system_of_units)
            base_multiplicand = Unit.get_base_units(for_unit.multiplicand, in_system_of_units)
            if base_multiplier is not None and base_multiplicand is not None:
                unit = Unit.get_unit_multiplication(base_multiplier, base_multiplicand,
                                                    system_of_units=in_system_of_units)
                return unit
        if isinstance(for_unit, UnitDivision):
            base_numerator = Unit.get_base_units(for_unit.numerator, in_system_of_units)
            base_denominator = Unit.get_base_units(for_unit.denominator, in_system_of_units)
            if base_numerator is not None and base_denominator is not None:
                unit = Unit.get_unit_division(base_numerator, base_denominator, system_of_units=in_system_of_units)
                return unit
        if isinstance(for_unit, UnitExponentiation):
            base_base = Unit.get_base_units(for_unit.base, in_system_of_units)
            if base_base is not None:
                unit = Unit.get_unit_exponentiation(base_base, for_unit.exponent, system_of_units=in_system_of_units)
                return unit
        raise UnitIdentityException("Cannot find a base unit for {} in the system of units: {}"
                                    .format(for_unit, in_system_of_units))

    @staticmethod
    def __add_when_not_duplicate(unit):
        result_unit = Unit.with_identifier(unit.identifier)
        dim_units = Unit.with_dimensions(unit.dimensions)
        if result_unit is None:
            for test_unit in dim_units:
                try:
                    conversion_factor = abs(Unit.conversion_factor(unit, test_unit) - 1.0)
                    if conversion_factor < 0.0000001:
                        result_unit = test_unit
                        break
                except UnitConversionException as error:
                    pass
        if result_unit is not None:
            if unit.label() is not None or unit.symbol() is not None:
                all_labels = result_unit.all_labels()
                found = False
                str_unit_label = str(unit.label())
                for label in all_labels:
                    if str(label) == str_unit_label:
                        found = True
                        break
                if not found:
                    result_unit = None
        if result_unit is None:
            result_unit = unit
            Unit._units.append(unit)
        return result_unit

    @staticmethod
    def simplified_compound_unit(unit):
        # creates a simplified unit (e.g. kg.mg/g becomes kg), conversion to this unit is however needed afterwards.
        if isinstance(unit, CompoundUnit):
            if isinstance(unit, UnitDivision):
                if not isinstance(unit.numerator, CompoundUnit) and not isinstance(unit.denominator, CompoundUnit) \
                        and unit.numerator.dimensions == unit.denominator.dimensions:
                    return unit

            exponents = Unit.__exponents_of_units_of_same_dimensions(unit)
            result = Unit.__create_unit_from_exponents(exponents)
            result.identifier = unit.identifier
            return result
        return unit

    @staticmethod
    def __exponents_of_units_of_same_dimensions(unit):
        # takes together the exponents of units that have the same dimensions.
        # So if you have kg.g this will result in exponent 2 for kg.
        reduced = []
        if isinstance(unit, CompoundUnit):
            bue = unit.get_units_exponents()
            for exp in bue:
                found = False
                for red in reduced:
                    if exp[0].dimensions == red[0].dimensions:
                        red[1] = red[1] + exp[1]
                        found = True
                if not found:
                    reduced.append([exp[0], exp[1]])
            return reduced
        reduced.append([unit, 1.0])
        return reduced

    @staticmethod
    def __create_unit_from_exponents(exponents):
        numerator = None
        denominator = None
        units_in_numerator = 0
        units_in_denominator = 0
        for red in exponents:
            if red[1] > 0:
                units_in_numerator += 1
            if red[1] < 0:
                units_in_denominator += 1
        for red in exponents:
            if red[1] > 0:
                unit_exp = red[0]
                if units_in_numerator <= 1 or str(
                        unit_exp.identifier) != "http://www.ontology-of-units-of-measure.org/resource/om-2/one":
                    if red[1] > 1:
                        unit_exp = UnitExponentiation(red[0], red[1])
                    if numerator is None:
                        numerator = unit_exp
                    else:
                        numerator = UnitMultiplication(numerator, unit_exp)
            if red[1] < 0:
                unit_exp = red[0]
                if unit_exp.identifier != str("http://www.ontology-of-units-of-measure.org/resource/om-2/one"):
                    if red[1] < -1:
                        unit_exp = UnitExponentiation(red[0], -red[1])
                    if denominator is None:
                        denominator = unit_exp
                    else:
                        denominator = UnitMultiplication(denominator, unit_exp)
        if numerator is None:
            numerator = Unit.get_singular_unit('one', '', Dimension())
        if denominator is None:
            result = numerator
        else:
            result = UnitDivision(numerator, denominator)
        return result

    @staticmethod
    def reduce_unit(unit):
        result = unit
        if isinstance(unit, CompoundUnit):
            reduced = []
            bue = unit.get_units_exponents()
            for exponent in bue:
                found = False
                for red in reduced:
                    if exponent[0] == red[0]:
                        found = True
                        red[1] = red[1] + exponent[1]
                if not found:
                    reduced.append(exponent)
            reduced.sort(key=Unit.cmp_to_key(Unit.__exponents_cmp))
            if isinstance(unit, UnitDivision) \
                    and not isinstance(unit.numerator, CompoundUnit) \
                    and not isinstance(unit.denominator, CompoundUnit) \
                    and unit.numerator.dimensions == unit.denominator.dimensions:
                return unit
            result = Unit.__create_unit_from_exponents(reduced)
            result.identifier = unit.identifier
        return result

    @staticmethod
    def __exponents_cmp(a, b):
        if a[1] > 0 and b[1] < 0:
            return -1
        if a[1] < 0 and b[1] > 0:
            return 1
        if a[1] > 0 and b[1] < a[1]:
            return 1
        if 0 < a[1] < b[1]:
            return -1
        if 0 > a[1] > b[1]:
            return -1
        if a[1] < 0 and b[1] > a[1]:
            return 1
        return 0

    @staticmethod
    def cmp_to_key(mycmp):
        'Convert a cmp= function into a key= function'

        class K:
            def __init__(self, obj, *args):
                self.obj = obj

            def __lt__(self, other):
                return mycmp(self.obj, other.obj) < 0

            def __gt__(self, other):
                return mycmp(self.obj, other.obj) > 0

            def __eq__(self, other):
                return mycmp(self.obj, other.obj) == 0

            def __le__(self, other):
                return mycmp(self.obj, other.obj) <= 0

            def __ge__(self, other):
                return mycmp(self.obj, other.obj) >= 0

            def __ne__(self, other):
                return mycmp(self.obj, other.obj) != 0

        return K

    @staticmethod
    def conversion_factor(from_unit, to_unit):
        can_convert = Unit.can_convert(from_unit, to_unit)
        if not can_convert:
            raise DimensionalException("A unit with dimensions {} cannot be converted to a unit with dimensions {}."
                                       .format(from_unit.dimensions, to_unit.dimensions))
        from_base_units_exponents = from_unit.get_base_units_exponents()
        to_base_units_exponents = to_unit.get_base_units_exponents()
        from_norm = Unit.__normalise_base_units_exponents(from_base_units_exponents)
        to_norm = Unit.__normalise_base_units_exponents(to_base_units_exponents)
        if not Unit.__check_same_units_from_normalised(from_norm, to_norm):
            raise UnitConversionException("Cannot convert from {} to {} as they do not have a common ancestor unit."
                                          .format(from_unit, to_unit))
        from_factor = Unit.__factor_from_normalised(from_norm)
        to_factor = Unit.__factor_from_normalised(to_norm)
        factor = from_factor / to_factor
        return factor

    @staticmethod
    def __normalise_base_units_exponents(base_unit_exponents):
        unit_dict = {}
        for exponent in base_unit_exponents:
            entry = [0, 1.0]
            if exponent[0].identifier in unit_dict:
                entry = unit_dict[exponent[0].identifier]
            entry[0] = entry[0] + exponent[1]
            entry[1] = entry[1] * pow(exponent[2], exponent[1])
            unit_dict[exponent[0].identifier] = entry
        return unit_dict

    @staticmethod
    def __factor_from_normalised(normalised):
        factor = 1.0
        for entry in normalised:
            factor = factor * normalised.get(entry)[1]
        return factor

    @staticmethod
    def __check_same_units_from_normalised(normalised_unit1, normalised_unit2):
        keys1 = normalised_unit1.keys()
        for ident in keys1:
            exp_1 = normalised_unit1.get(ident)
            exp_2 = normalised_unit2.get(ident)
            if exp_2 is None or exp_1[0] != exp_2[0]:
                return False
        keys2 = normalised_unit2.keys()
        for ident in keys2:
            exp_1 = normalised_unit1.get(ident)
            exp_2 = normalised_unit2.get(ident)
            if exp_1 is None or exp_1[0] != exp_2[0]:
                return False
        return True

    @staticmethod
    def print_base_units_exponents(unit):
        base_units_exponents = unit.get_base_units_exponents()
        bue_string = ""
        i = 0
        for exponent in base_units_exponents:
            bue_string += f'[{str(exponent[0].symbol()), exponent[1], exponent[2]}]'
            i = i + 1
            if i < len(base_units_exponents):
                bue_string += ', '

        print('BASE UNIT EXPONENTS: {}: [{}]'.format(unit.symbol(), bue_string))

    @staticmethod
    def can_convert(from_unit, to_unit):
        if isinstance(from_unit, Unit) and isinstance(to_unit, Unit):
            if from_unit.dimensions == to_unit.dimensions:
                return True
            return False
        if not isinstance(from_unit, Unit) and not isinstance(to_unit, Unit):
            raise ValueError("Both arguments of can_convert are required to be of type Unit.")
        if not isinstance(from_unit, Unit):
            raise ValueError("The first argument of can_convert is required to be of type Unit.")
        if not isinstance(to_unit, Unit):
            raise ValueError("The second argument of can_convert is required to be of type Unit.")

    @staticmethod
    def get_singular_unit(label, symbol, dimensions=Dimension(), base_unit=None, factor=1.0, identifier=None,
                          cache=True, system_of_units=None, is_base_unit=False):
        unit = None
        if identifier is not None:
            test_unit = Unit.with_identifier(identifier)
            if test_unit is not None:
                if not isinstance(test_unit, SingularUnit):
                    raise UnitIdentityException("The identifier for a SingularUnit has been used earlier"
                                                " for another type of unit")
                else:
                    if base_unit is not None and base_unit != test_unit.baseUnit:
                        base_identifier = str(base_unit.identifier)
                        test_identifier = str(test_unit.baseUnit.identifier)
                        base_ok = False
                        factor_ok = False
                        if base_identifier == test_identifier:
                            base_ok = True
                            if factor == test_unit.factor:
                                factor_ok = True
                        else:
                            can_convert_base = Unit.can_convert(base_unit, test_unit.baseUnit)
                            if can_convert_base:
                                base_ok = True
                                conv_factor = Unit.conversion_factor(base_unit, test_unit.baseUnit)
                                tot_factor = conv_factor * factor
                                log_tot_factor = math.log10(tot_factor)
                                log_factor = math.log10(test_unit.factor)
                                diff = abs(log_tot_factor - log_factor)
                                if diff < 0.00001:
                                    factor_ok = True
                        if not base_ok:
                            raise UnitIdentityException("The requested SingularUnit uses a different base unit"
                                                        " as the earlier defined unit with the same identifier.")
                        if not factor_ok:
                            raise UnitIdentityException("The requested SingularUnit uses a different conversion factor"
                                                        " as the earlier defined unit with the same identifier.")
                if label is not None:
                    test_unit.add_preferred_label(label)
                if symbol is not None:
                    test_unit.add_symbol(symbol)
                if system_of_units is not None and test_unit.systemOfUnits is None:
                    test_unit.systemOfUnits = system_of_units
                if is_base_unit:
                    test_unit.isBaseUnit = is_base_unit
                return test_unit
        if unit is None:
            if base_unit is not None and base_unit.systemOfUnits is not None and system_of_units is None \
                    and factor == 1.0:
                system_of_units = base_unit.systemOfUnits
            unit = SingularUnit(label, symbol, dimensions, base_unit, factor, identifier, system_of_units, is_base_unit)
            if cache:
                unit = Unit.__add_when_not_duplicate(unit)
        return unit

    @staticmethod
    def get_prefixed_unit(prefix, base_unit, identifier=None, cache=True, system_of_units=None, is_base_unit=False):
        unit = None
        if isinstance(prefix, str):
            prefix = Prefix.with_identifier(prefix)
        if isinstance(prefix, URIRef):
            prefix = Prefix.with_identifier(prefix.value)
        if identifier is not None:
            test_unit = Unit.with_identifier(identifier)
            if test_unit is not None:
                if not isinstance(test_unit, PrefixedUnit):
                    raise UnitIdentityException("The identifier for a PrefixedUnit has been used earlier"
                                                " for another type of unit")
                else:
                    base_identifier = str(base_unit.identifier)
                    test_identifier = str(test_unit.baseUnit.identifier)
                    if base_identifier != test_identifier:
                        raise UnitIdentityException("The requested PrefixedUnit uses a different base unit"
                                                    " as the earlier defined unit with the same identifier.")
                    if str(prefix.identifier) != str(test_unit.prefix.identifier):
                        raise UnitIdentityException("The requested PrefixedUnit uses a different prefix"
                                                    " as the earlier defined unit with the same identifier.")
                if system_of_units is not None and test_unit.systemOfUnits is None:
                    test_unit.systemOfUnits = system_of_units
                if base_unit.systemOfUnits is not None and test_unit.systemOfUnits is None:
                    test_unit.systemOfUnits = base_unit.systemOfUnits
                if is_base_unit:
                    test_unit.isBaseUnit = is_base_unit
                return test_unit
        if unit is None:
            test_unit = Unit.with_prefix(prefix, base_unit)
            if test_unit is not None:
                if identifier is not None and not isinstance(test_unit.identifier, URIRef):
                    test_unit.identifier = URIRef(identifier)
                if system_of_units is not None and test_unit.systemOfUnits is None:
                    test_unit.systemOfUnits = system_of_units
                if base_unit.systemOfUnits is not None and test_unit.systemOfUnits is None:
                    test_unit.systemOfUnits = base_unit.systemOfUnits
                if is_base_unit and not test_unit.isBaseUnit:
                    test_unit.isBaseUnit = is_base_unit
                return test_unit
        if unit is None:
            if system_of_units is None:
                system_of_units = base_unit.systemOfUnits
            unit = PrefixedUnit(prefix, base_unit, identifier, system_of_units=system_of_units,
                                is_base_unit=is_base_unit)
            if cache:
                unit = Unit.__add_when_not_duplicate(unit)
        return unit

    @staticmethod
    def get_unit_multiple(base_unit, factor=1.0, identifier=None, label=None, symbol=None, cache=True,
                          system_of_units=None):
        unit = None
        if identifier is not None:
            test_unit = Unit.with_identifier(identifier)
            if test_unit is not None:
                if not isinstance(test_unit, UnitMultiple):
                    raise UnitIdentityException("The identifier for a UnitMultiple has been used earlier"
                                                " for another type of unit")
                else:
                    base_identifier = str(base_unit.identifier)
                    test_identifier = str(test_unit.baseUnit.identifier)
                    if base_identifier != test_identifier:
                        raise UnitIdentityException("The requested UnitMultiple uses a different base unit"
                                                    " as the earlier defined unit with the same identifier.")
                if label is not None:
                    test_unit.add_preferred_label(label)
                if symbol is not None:
                    test_unit.add_symbol(symbol)
                if system_of_units is not None and test_unit.systemOfUnits is None:
                    test_unit.systemOfUnits = system_of_units
                if base_unit.systemOfUnits is not None and test_unit.systemOfUnits is None:
                    test_unit.systemOfUnits = base_unit.systemOfUnits
                return test_unit
        if unit is None:
            unit = UnitMultiple(base_unit, factor, identifier, label, symbol, system_of_units)
            if system_of_units is None:
                system_of_units = base_unit.systemOfUnits
            if cache:
                unit = Unit.__add_when_not_duplicate(unit)
        return unit

    @staticmethod
    def get_unit_multiplication(multiplier, multiplicand, identifier=None, cache=True,
                                system_of_units=None):
        if identifier is not None:
            for test_unit in Unit._units:
                if str(test_unit.identifier) == str(identifier):
                    if not isinstance(test_unit, UnitMultiplication):
                        raise UnitIdentityException("The identifier for a UnitMultiplication has been used earlier"
                                                    " for another type of unit")
                    else:
                        if str(multiplier.identifier) != str(test_unit.multiplier.identifier) or \
                                str(multiplicand.identifier) != str(test_unit.multiplicand.identifier):
                            raise UnitIdentityException("The requested UnitMultiplication uses a different pair of"
                                                        " units as multiplier and multiplicand as the earlier defined"
                                                        " unit with the same identifier.")
                    if system_of_units is not None and test_unit.systemOfUnits is None:
                        test_unit.systemOfUnits = system_of_units
                    if multiplier.systemOfUnits is not None and multiplicand.systemOfUnits is not None \
                            and multiplier.systemOfUnits == multiplicand.systemOfUnits \
                            and test_unit.systemOfUnits is None:
                        test_unit.systemOfUnits = multiplier.systemOfUnits
                    return test_unit
        unit = Unit.with_multiplication(multiplier, multiplicand)
        if unit is not None:
            if identifier is not None:
                unit.identifier = identifier
                if system_of_units is not None and unit.systemOfUnits is None:
                    unit.systemOfUnits = system_of_units
                if multiplier.systemOfUnits is not None and multiplicand.systemOfUnits is not None \
                        and multiplier.systemOfUnits == multiplicand.systemOfUnits and unit.systemOfUnits is None:
                    unit.systemOfUnits = multiplier.systemOfUnits
        if unit is None:
            for unit in Unit._units:
                if isinstance(unit, UnitMultiplication):
                    if str(multiplier.identifier) == str(unit.multiplier.identifier) and \
                            str(multiplicand.identifier) == str(unit.multiplicand.identifier):
                        if system_of_units is not None:
                            unit.systemOfUnits = system_of_units
                        return unit
            if system_of_units is None and multiplier.systemOfUnits is not None and \
                    multiplicand.systemOfUnits is not None and multiplicand.systemOfUnits == multiplier.systemOfUnits:
                system_of_units = multiplier.systemOfUnits
            unit = UnitMultiplication(multiplier, multiplicand, identifier, system_of_units)
            unit = Unit.reduce_unit(unit)
            if cache:
                unit = Unit.__add_when_not_duplicate(unit)
        return unit

    @staticmethod
    def get_unit_division(numerator, denominator, identifier=None, cache=True, system_of_units=None):
        if identifier is not None:
            for test_unit in Unit._units:
                if str(test_unit.identifier) == str(identifier):
                    if not isinstance(test_unit, UnitDivision):
                        raise UnitIdentityException("The identifier for a UnitDivision has been used earlier"
                                                    " for another type of unit")
                    else:
                        if str(numerator.identifier) != str(test_unit.numerator.identifier) or \
                                str(denominator.identifier) != str(test_unit.denominator.identifier):
                            raise UnitIdentityException("The requested UnitDivision uses a different pair of"
                                                        " units as numerator and denominator as the earlier "
                                                        "defined unit with the same identifier.")
                    if system_of_units is not None and test_unit.systemOfUnits is None:
                        test_unit.systemOfUnits = system_of_units
                    if numerator.systemOfUnits is not None and denominator.systemOfUnits is not None \
                            and numerator.systemOfUnits == denominator.systemOfUnits \
                            and test_unit.systemOfUnits is None:
                        test_unit.systemOfUnits = numerator.systemOfUnits
                    return test_unit
        unit = Unit.with_division(numerator, denominator)
        if unit is not None:
            if identifier is not None:
                unit.identifier = identifier
            if system_of_units is not None:
                unit.systemOfUnits = system_of_units
            if numerator.systemOfUnits is not None and denominator.systemOfUnits is not None \
                    and numerator.systemOfUnits == denominator.systemOfUnits and unit.systemOfUnits is None:
                unit.systemOfUnits = numerator.systemOfUnits
        if unit is None:
            for unit in Unit._units:
                if isinstance(unit, UnitDivision):
                    if str(numerator.identifier) == str(unit.numerator.identifier) and \
                            str(denominator.identifier) == str(unit.denominator.identifier):
                        if system_of_units is not None:
                            unit.systemOfUnits = system_of_units
                        return unit
            if system_of_units is None and denominator.systemOfUnits is not None and \
                    numerator.systemOfUnits is not None and numerator.systemOfUnits == denominator.systemOfUnits:
                system_of_units = numerator.systemOfUnits
            unit = UnitDivision(numerator, denominator, identifier, system_of_units)
            unit = Unit.reduce_unit(unit)
            if cache:
                unit = Unit.__add_when_not_duplicate(unit)
        return unit

    @staticmethod
    def get_unit_exponentiation(base, exponent, identifier=None, cache=True, system_of_units=None):
        unit = None
        if identifier is not None:
            for test_unit in Unit._units:
                if str(test_unit.identifier) == str(identifier):
                    if not isinstance(test_unit, UnitExponentiation):
                        raise UnitIdentityException("The identifier for a UnitExponentiation has been used earlier"
                                                    " for another type of unit")
                    else:
                        if str(base.identifier) != str(test_unit.base.identifier):
                            raise UnitIdentityException("The requested UnitExponentiation uses a different unit "
                                                        "as base as the earlier defined unit with the same identifier.")
                        if exponent != test_unit.exponent:
                            raise UnitIdentityException("The requested UnitExponentiation uses a different exponent as"
                                                        " the earlier defined unit with the same identifier.")
                    if system_of_units is not None and test_unit.systemOfUnits is None:
                        test_unit.systemOfUnits = system_of_units
                    if base.systemOfUnits is not None and test_unit.systemOfUnits is None:
                        test_unit.systemOfUnits = base.systemOfUnits
                    return test_unit
        unit = Unit.with_exponentiation(base, exponent)
        if unit is not None:
            if identifier is not None:
                unit.identifier = identifier
            if system_of_units is not None:
                unit.systemOfUnits = system_of_units
        if unit is None:
            for unit in Unit._units:
                if isinstance(unit, UnitExponentiation):
                    if str(base.identifier) == str(unit.base.identifier) and \
                            exponent == unit.exponent:
                        if system_of_units is not None:
                            unit.systemOfUnits = system_of_units
                        return unit
            if system_of_units is None and base.systemOfUnits is not None:
                system_of_units = base.systemOfUnits
            unit = UnitExponentiation(base, exponent, identifier, system_of_units)
            unit = Unit.reduce_unit(unit)
            if cache:
                unit = Unit.__add_when_not_duplicate(unit)
        return unit

    def __init__(self, label=None, symbol=None, dimensions=Dimension(), identifier=None, system_of_units=None,
                 is_base_unit=False):
        super().__init__(label, symbol, identifier)
        self.isBaseUnit = is_base_unit
        self.systemOfUnits = system_of_units
        self.dimensions = dimensions

    def __str__(self):
        return f'{self.label()}\t{self.symbol()}\t<{self.identifier}>  dim: {self.dimensions}'

    def __eq__(self, other):
        if isinstance(other, Unit):
            return str(self.identifier) == str(other.identifier)
        return False

    def __ne__(self, other):
        return not (self == other)


class Prefix(object):
    _prefixes = dict()

    @staticmethod
    def with_identifier(identifier):
        identifier_string = str(identifier)
        if identifier_string in Prefix._prefixes:
            return Prefix._prefixes[identifier_string]
        return None

    def __init__(self, name, symbol, factor, identifier):
        self.name = name
        self.symbol = symbol
        self.factor = factor
        self.identifier = identifier
        Prefix._prefixes[str(identifier)] = self

    def __eq__(self, other):
        if isinstance(other, Prefix):
            return str(self.identifier) == str(other.identifier)
        return False

    def __ne__(self, other):
        return not (self == other)


class SingularUnit(Unit):

    def __init__(self, label, symbol, dimensions=Dimension(), base_unit=None, factor=1.0, identifier=None,
                 system_of_units=None, is_base_unit=False):
        if base_unit is not None:
            dimensions = base_unit.dimensions
        if system_of_units is None and not is_base_unit and base_unit is not None and factor == 1.0:
            system_of_units = base_unit.systemOfUnits
        super().__init__(label, symbol, dimensions, identifier, system_of_units=system_of_units,
                         is_base_unit=is_base_unit)
        self.factor = factor
        self.baseUnit = base_unit

    def __eq__(self, other):
        if isinstance(other, SingularUnit):
            if not isinstance(self.identifier, URIRef) or not isinstance(other.identifier, URIRef):
                if self.baseUnit is None and other.baseUnit == self and other.factor == self.factor:
                    return True
                if other.baseUnit is None and self.baseUnit == other and other.factor == self.factor:
                    return True
                if other.baseUnit == self.baseUnit and other.factor == self.factor:
                    return True
            else:
                return str(self.identifier) == str(other.identifier)
        return False

    def get_units_exponents(self):
        return [[self, 1, 1.0]]

    def get_base_units_exponents(self):
        if self.baseUnit is None:
            return [[self, 1, 1.0]]
        base_base = self.baseUnit.get_base_units_exponents()
        result = []
        factor_added = False
        for exponents in base_base:
            if not factor_added:
                corrected_factor = pow(self.factor, 1 / (exponents[1]))
                converted = [exponents[0], exponents[1], exponents[2] * corrected_factor]
                factor_added = True
            else:
                converted = exponents
            result.append(converted)
        return result


class PrefixedUnit(Unit):

    def __init__(self, prefix, base_unit, identifier=None, system_of_units=None, is_base_unit=False):
        label = f'{prefix.name}{base_unit.label()}'
        symbol = f'{prefix.symbol}{base_unit.symbol()}'
        self.prefix = prefix
        self.baseUnit = base_unit
        if system_of_units is None and not is_base_unit and base_unit is not None:
            system_of_units = base_unit.systemOfUnits
        super().__init__(label, symbol, dimensions=self.baseUnit.dimensions, identifier=identifier,
                         system_of_units=system_of_units, is_base_unit=is_base_unit)

    def __eq__(self, other):
        if isinstance(other, PrefixedUnit):
            if not isinstance(self.identifier, URIRef) or not isinstance(other.identifier, URIRef):
                if other.baseUnit == self.baseUnit and other.prefix == self.prefix:
                    return True
            else:
                return str(self.identifier) == str(other.identifier)
        return False

    def get_units_exponents(self):
        return [[self, 1, 1.0]]

    def get_base_units_exponents(self):
        base_base = self.baseUnit.get_base_units_exponents()
        result = []
        factor_added = False
        for exponents in base_base:
            if not factor_added:
                corrected_factor = pow(self.prefix.factor, 1 / (exponents[1]))
                converted = [exponents[0], exponents[1], exponents[2] * corrected_factor]
                factor_added = True
            else:
                converted = exponents
            result.append(converted)
        return result


class UnitMultiple(SingularUnit):

    def __init__(self, base_unit, factor=1.0, identifier=None, label=None, symbol=None, system_of_units=None,
                 is_base_unit=False):
        if label is None:
            label = f'{factor} {base_unit.label()}'
        if symbol is None:
            symbol = f'{factor}{base_unit.symbol()}'
        super().__init__(label, symbol, base_unit.dimensions, base_unit, factor, identifier,
                         system_of_units=system_of_units, is_base_unit=is_base_unit)


class CompoundUnit(Unit):

    def __init__(self, symbol, dimensions, identifier=None, system_of_units=None, is_base_unit=False):
        super().__init__(None, symbol, dimensions, identifier, system_of_units=system_of_units,
                         is_base_unit=is_base_unit)


class UnitMultiplication(CompoundUnit):

    def __init__(self, multiplier, multiplicand, identifier=None, system_of_units=None,
                 is_base_unit=False):
        dimensions = multiplier.dimensions * multiplicand.dimensions
        multiplier_str = str(multiplier.symbol())
        if isinstance(multiplier, CompoundUnit) and not isinstance(multiplier, UnitExponentiation):
            multiplier_str = f'{multiplier_str}'
        multiplicand_str = str(multiplicand.symbol())
        if isinstance(multiplicand, CompoundUnit) and not isinstance(multiplicand, UnitExponentiation):
            multiplicand_str = f'{multiplicand_str}'
        if system_of_units is None and multiplier.systemOfUnits is not None and multiplicand.systemOfUnits is not None \
                and multiplier.systemOfUnits == multiplicand.systemOfUnits:
            system_of_units = multiplier.systemOfUnits
        symbol = f'{multiplier_str}·{multiplicand_str}'
        super().__init__(symbol, dimensions, identifier, system_of_units=system_of_units, is_base_unit=is_base_unit)
        self.multiplier = multiplier
        self.multiplicand = multiplicand

    def __eq__(self, other):
        if isinstance(other, UnitMultiplication):
            if not isinstance(self.identifier, URIRef) or not isinstance(other.identifier, URIRef):
                if other.multiplier == self.multiplier and other.multiplicand == self.multiplicand:
                    return True
                if other.multiplier == self.multiplicand and other.multiplicand == self.multiplier:
                    return True
            else:
                return str(self.identifier) == str(other.identifier)
        return False

    def get_units_exponents(self):
        multiplier_exp = self.multiplier.get_units_exponents()
        multiplicand_exp = self.multiplicand.get_units_exponents()
        result = []
        result.extend(multiplier_exp)
        result.extend(multiplicand_exp)
        return result

    def get_base_units_exponents(self):
        multiplier_base = self.multiplier.get_base_units_exponents()
        multiplicand_base = self.multiplicand.get_base_units_exponents()
        result = []
        result.extend(multiplier_base)
        result.extend(multiplicand_base)
        return result


class UnitDivision(CompoundUnit):

    def __init__(self, numerator, denominator, identifier=None, system_of_units=None, is_base_unit=False):
        dimensions = numerator.dimensions / denominator.dimensions
        numerator_str = str(numerator.symbol())
        if isinstance(numerator, CompoundUnit) and not isinstance(numerator, UnitExponentiation):
            numerator_str = f'({numerator_str})'
        denominator_str = str(denominator.symbol())
        if isinstance(denominator, CompoundUnit) and not isinstance(denominator, UnitExponentiation):
            denominator_str = f'({denominator_str})'
        if system_of_units is None and numerator.systemOfUnits is not None and denominator.systemOfUnits is not None \
                and numerator.systemOfUnits == denominator.systemOfUnits:
            system_of_units = numerator.systemOfUnits
        symbol = f'{numerator_str}/{denominator_str}'
        super().__init__(symbol, dimensions, identifier, system_of_units=system_of_units, is_base_unit=is_base_unit)
        self.numerator = numerator
        self.denominator = denominator

    def __eq__(self, other):
        if isinstance(other, UnitDivision):
            if not isinstance(self.identifier, URIRef) or not isinstance(other.identifier, URIRef):
                if other.numerator == self.numerator and other.denominator == self.denominator:
                    return True
            else:
                return str(self.identifier) == str(other.identifier)
        return False

    def get_units_exponents(self):
        numerator_exp = self.numerator.get_units_exponents()
        denominator_exp = self.denominator.get_units_exponents()
        result = []
        result.extend(numerator_exp)
        for exponents in denominator_exp:
            result.append([exponents[0], -exponents[1], exponents[2]])
        return result

    def get_base_units_exponents(self):
        numerator_base = self.numerator.get_base_units_exponents()
        denominator_base = self.denominator.get_base_units_exponents()
        result = []
        result.extend(numerator_base)
        for exponents in denominator_base:
            result.append([exponents[0], -exponents[1], exponents[2]])
        return result


class UnitExponentiation(CompoundUnit):

    def __init__(self, base, exponent, identifier=None, system_of_units=None, is_base_unit=False):
        dimensions = Dimension.pow(base.dimensions, exponent)
        base_str = str(base.symbol())
        if isinstance(base, CompoundUnit):
            base_str = f'({base_str})'
        if system_of_units is None and base.systemOfUnits is not None:
            system_of_units = base.systemOfUnits
        symbol = f'{base_str}{exponent}'
        super().__init__(symbol, dimensions, identifier, system_of_units=system_of_units, is_base_unit=is_base_unit)
        self.base = base
        self.exponent = exponent

    def __eq__(self, other):
        if isinstance(other, UnitExponentiation):
            if not isinstance(self.identifier, URIRef) or not isinstance(other.identifier, URIRef):
                if other.base == self.base and other.exponent == self.exponent:
                    return True
            else:
                return str(self.identifier) == str(other.identifier)
        return False

    def get_units_exponents(self):
        base_exp = self.base.get_units_exponents()
        result = []
        for exponents in base_exp:
            result.append([exponents[0], exponents[1] * self.exponent, exponents[2]])
        return result

    def get_base_units_exponents(self):
        base_base = self.base.get_base_units_exponents()
        result = []
        for exponents in base_base:
            result.append([exponents[0], exponents[1] * self.exponent, exponents[2]])
        return result
