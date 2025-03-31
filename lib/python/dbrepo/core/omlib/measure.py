import math

from dbrepo.core.omlib.constants import SI
from dbrepo.core.omlib.exceptions.dimensionexception import DimensionalException
from dbrepo.core.omlib.scale import Scale
from dbrepo.core.omlib.thing import Thing
from dbrepo.core.omlib.unit import Unit, PrefixedUnit, SingularUnit


def om(numerical_value, unit_or_scale, identifier=None):
    if isinstance(unit_or_scale, Unit):
        return Measure(numerical_value, unit_or_scale, identifier)
    if isinstance(unit_or_scale, Scale):
        return Point(numerical_value, unit_or_scale, identifier)
    return None


class Point(Thing):

    @staticmethod
    def create_by_converting(point, to_scale):
        if not isinstance(point, Point):
            raise ValueError("The parameter to the convert method is not of the correct type (Point).")
        if not isinstance(to_scale, Scale):
            raise ValueError("The parameter to the convert method is not of the correct type (Scale).")
        new_point = Point(point.numericalValue, point.scale)
        new_point.convert(to_scale)
        return new_point

    @staticmethod
    def create_by_converting_to_ratio_scale(point):
        if not isinstance(point, Point):
            raise ValueError("The parameter to the convert method is not of the correct type (Point).")
        new_point = Point(point.numericalValue, point.scale)
        new_point.convert_to_ratio_scale()
        return new_point

    def __init__(self, numerical_value, scale, identifier=None):
        super().__init__(identifier=identifier)
        self.numericalValue = numerical_value
        self.scale = scale

    def convert(self, to_scale):
        if not isinstance(to_scale, Scale):
            raise ValueError("The parameter to the convert method is not of the correct type (Scale).")
        factor = Scale.conversion_factor(self.scale, to_scale)
        off_set = Scale.conversion_off_set(self.scale, to_scale)
        self.numericalValue = self.numericalValue * factor + off_set
        self.scale = to_scale

    def convert_to_ratio_scale(self):
        base = self.scale.base_ratio_scale()
        factor = Scale.conversion_factor(self.scale, base[0])
        off_set = Scale.conversion_off_set(self.scale, base[0])
        self.numericalValue = self.numericalValue * factor + off_set
        self.scale = base[0]

    def __str__(self):
        return f'{self.numericalValue} {self.scale.unit.symbol()}'

    def __new_value_for_comparisson(self, other):
        if isinstance(other, Point):
            factor = Scale.conversion_factor(self.scale, other.scale)
            off_set = Scale.conversion_off_set(self.scale, other.scale)
            new_value = self.numericalValue * factor + off_set
            return new_value
        return None

    def __eq__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value == other.numericalValue
        return False

    def __ne__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value != other.numericalValue
        return False

    def __lt__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value < other.numericalValue
        return False

    def __le__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value <= other.numericalValue
        return False

    def __gt__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value > other.numericalValue
        return False

    def __ge__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value >= other.numericalValue
        return False

    def __add__(self, other):
        if not isinstance(other, Measure):
            raise ValueError('The value to be added is not a measure and only measures can be added to a point.')
        if not self.scale.dimensions == other.unit.dimensions:
            raise DimensionalException("Measures and Points with units of different dimensions cannot be added "
                                       "together. {} != {}"
                                       .format(self.scale.unit, other.unit))
        new_measure = Measure.create_by_converting(other, self.scale.unit)
        return_point = Point(self.numericalValue + new_measure.numericalValue, self.scale)
        return return_point

    def __sub__(self, other):
        if not isinstance(other, Measure) and not isinstance(other, Point):
            raise ValueError('The value to be subtracted is not a point or a measure and only measures or points '
                             'can be subtracted from a point.')
        if isinstance(other, Measure):
            if not self.scale.dimensions == other.unit.dimensions:
                raise DimensionalException("Measures and Points with units of different dimensions cannot be "
                                           "subtracted from each other. {} != {}".format(self.scale.unit, other.unit))
            new_measure = Measure.create_by_converting(other, self.scale.unit)
            return_point = Point(self.numericalValue - new_measure.numericalValue, self.scale)
            return return_point
        if isinstance(other, Point):
            if not self.scale.dimensions == other.scale.dimensions:
                raise DimensionalException("Measures and Points with units of different dimensions cannot be "
                                           "subtracted from each other. {} != {}".format(self.scale.unit, other.unit))
            new_point = Point.create_by_converting(other, self.scale)
            return_measure = Measure(self.numericalValue - new_point.numericalValue, self.scale.unit)
            return return_measure

    def __mul__(self, other):
        as_measure = Measure(self.numericalValue, self.scale.unit)
        return as_measure * other

    def __truediv__(self, other):
        as_measure = Measure(self.numericalValue, self.scale.unit)
        return as_measure / other


class Measure(Thing):

    @staticmethod
    def create_by_converting(measure, to_unit):
        if not isinstance(measure, Measure):
            raise ValueError("The parameter to the convert method is not of the correct type (Measure).")
        if not isinstance(to_unit, Unit):
            raise ValueError("The parameter to the convert method is not of the correct type (Unit).")
        new_measure = Measure(measure.numericalValue, measure.unit)
        new_measure.convert(to_unit)
        return new_measure

    @staticmethod
    def create_by_converting_to_base_units(measure, in_system_of_units=SI):
        if not isinstance(measure, Measure):
            raise ValueError("The parameter to the convert method is not of the correct type (Measure).")
        new_measure = Measure(measure.numericalValue, measure.unit)
        new_measure.convert_to_base_units(in_system_of_units)
        return new_measure

    @staticmethod
    def create_by_converting_to_convenient_units(measure, in_system_of_units=None, use_prefixes=True):
        if not isinstance(measure, Measure):
            raise ValueError("The parameter to the convert method is not of the correct type (Measure).")
        new_measure = Measure(measure.numericalValue, measure.unit)
        new_measure.convert_to_convenient_units(in_system_of_units, use_prefixes=use_prefixes)
        return new_measure

    def __init__(self, numerical_value, unit, identifier=None):
        super().__init__(identifier=identifier)
        self.numericalValue = numerical_value
        self.unit = unit

    def convert(self, to_unit):
        if not isinstance(to_unit, Unit):
            raise ValueError("The parameter to the convert method is not of the correct type (Unit).")
        factor = Unit.conversion_factor(self.unit, to_unit)
        self.numericalValue = self.numericalValue * factor
        self.unit = to_unit

    def convert_to_base_units(self, in_system_of_units=None):
        if in_system_of_units is None:
            in_system_of_units = self.unit.systemOfUnits
        if in_system_of_units is None:
            in_system_of_units = SI.SYSTEM_OF_UNITS
        base = Unit.get_base_units(self.unit, in_system_of_units)
        self.convert(base)

    def convert_to_convenient_units(self, system_of_units=None, use_prefixes=True):
        if system_of_units is None:
            system_of_units = self.unit.systemOfUnits
        test_units = self.unit.with_dimensions(self.unit.dimensions, in_system_of_units=system_of_units)
        selected_unit = self.unit
        log_selected_value = Measure.__get_log_value(self.numericalValue, selected_unit)
        for test_unit in test_units:
            if use_prefixes or not isinstance(test_unit, PrefixedUnit):
                factor = Unit.conversion_factor(self.unit, test_unit)
                value = abs(self.numericalValue * factor)
                log_value = Measure.__get_log_value(value, test_unit)
                if log_value < log_selected_value:
                    log_selected_value = log_value
                    selected_unit = test_unit
                if abs(log_value - log_selected_value) < 0.1 and isinstance(test_unit, SingularUnit):
                    log_selected_value = log_value
                    selected_unit = test_unit
                if abs(log_value - log_selected_value) < 0.1 and isinstance(test_unit, PrefixedUnit)\
                        and not isinstance(selected_unit, SingularUnit):
                    log_selected_value = log_value
                    selected_unit = test_unit
        self.convert(selected_unit)

    @staticmethod
    def __get_log_value(value, unit):
        if value == 0.0:
            return 0
        log_value = abs(math.log10(value))
        if value < 1:
            log_value = log_value + 2
        if not isinstance(unit, SingularUnit):
            log_value = log_value + 1
        return log_value

    def __str__(self):
        return f'{self.numericalValue} {self.unit.symbol()}'

    def __new_value_for_comparisson(self, other):
        if isinstance(other, Measure):
            factor = Unit.conversion_factor(self.unit, other.unit)
            new_value = self.numericalValue * factor
            return new_value
        return None

    def __eq__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value == other.numericalValue
        return False

    def __ne__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value != other.numericalValue
        return False

    def __lt__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value < other.numericalValue
        return False

    def __le__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value <= other.numericalValue
        return False

    def __gt__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value > other.numericalValue
        return False

    def __ge__(self, other):
        new_value = self.__new_value_for_comparisson(other)
        if new_value is not None:
            return new_value >= other.numericalValue
        return False

    def __add__(self, other):
        if not isinstance(other, Measure):
            raise ValueError('The value to be added is not a measure.')
        if not self.unit.dimensions == other.unit.dimensions:
            raise DimensionalException("Measures with units of different dimensions cannot be added together. {} != {}"
                                       .format(self.unit, other.unit))
        new_measure = Measure.create_by_converting(other, self.unit)
        return_measure = Measure(self.numericalValue + new_measure.numericalValue, self.unit)
        return return_measure

    def __sub__(self, other):
        if not isinstance(other, Measure):
            raise ValueError('The value to be subtracted is not a measure.')
        if not self.unit.dimensions == other.unit.dimensions:
            raise DimensionalException("Measures with units of different dimensions cannot be subtracted from each "
                                       "other. {} != {}".format(self.unit, other.unit))
        new_measure = Measure.create_by_converting(other, self.unit)
        return_measure = Measure(self.numericalValue - new_measure.numericalValue, self.unit)
        return return_measure

    def __mul__(self, other):
        if isinstance(other, float) or isinstance(other, int):
            new_measure = Measure(self.numericalValue * other, self.unit)
            return new_measure
        if not isinstance(other, Measure) and not isinstance(other, Point):
            raise ValueError('The multiplicand is not a measure, a point, a float, or an int.')
        other_unit = None
        if isinstance(other, Measure):
            other_unit = other.unit
        if isinstance(other, Point):
            other_unit = other.scale.unit
        new_value = self.numericalValue * other.numericalValue
        new_unit = Unit.get_unit_multiplication(self.unit, other_unit)
        new_measure = Measure(new_value, new_unit)
        simple_unit = Unit.simplified_compound_unit(new_unit)
        new_measure.convert(simple_unit)
        return new_measure

    def __truediv__(self, other):
        if isinstance(other, float) or isinstance(other, int):
            new_measure = Measure(self.numericalValue / other, self.unit)
            return new_measure
        if not isinstance(other, Measure) and not isinstance(other, Point):
            raise ValueError('The denominator is not a measure, a point, a float, or an int.')
        other_unit = None
        if isinstance(other, Measure):
            other_unit = other.unit
        if isinstance(other, Point):
            other_unit = other.scale.unit
        new_value = self.numericalValue / other.numericalValue
        new_unit = Unit.get_unit_division(self.unit, other_unit)
        new_measure = Measure(new_value, new_unit)
        simple_unit = Unit.simplified_compound_unit(new_unit)
        new_measure.convert(simple_unit)
        return new_measure
