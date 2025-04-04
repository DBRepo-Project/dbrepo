from rdflib import URIRef

from dbrepo.core.omlib.dimension import Dimension
from dbrepo.core.omlib.exceptions.dimensionexception import DimensionalException
from dbrepo.core.omlib.exceptions.unitconversionexception import ScaleConversionException
from dbrepo.core.omlib.exceptions.unitidentityexception import ScaleIdentityException
from dbrepo.core.omlib.thing import Thing
from dbrepo.core.omlib.unit import Unit


class Scale(Thing):
    _scales = []

    @staticmethod
    def clear_cache():
        Scale._scales.clear()

    @staticmethod
    def _add_when_not_in_cache(scale):
        for test_scale in Scale._scales:
            if test_scale == scale:
                if test_scale.identifier == scale.identifier or not isinstance(scale.identifier, URIRef) \
                        or not isinstance(test_scale.identifier, URIRef):
                    if isinstance(test_scale, RatioScale) and isinstance(scale, RatioScale):
                        if test_scale.unit == scale.unit:
                            if isinstance(scale.identifier, URIRef) and not isinstance(test_scale.identifier, URIRef):
                                test_scale.identifier = scale.identifier
                            return test_scale
                    if isinstance(test_scale, IntervalScale) and isinstance(scale, IntervalScale):
                        if test_scale.unit == scale.unit and test_scale.baseScale == scale.baseScale \
                                and test_scale.offSet == scale.offSet:
                            if isinstance(scale.identifier, URIRef) and not isinstance(test_scale.identifier, URIRef):
                                test_scale.identifier = scale.identifier
                            return test_scale
                    if test_scale.identifier == scale.identifier:
                        raise ScaleIdentityException("The scale with identifier {} already exists but with different"
                                                     "properties. \n{} \nis not the same as \n{}".format(scale.identifier,
                                                     scale, test_scale))
        Scale._scales.append(scale)
        return scale

    @staticmethod
    def with_label(label):
        return_scales = []
        for test_scale in Scale._scales:
            for test_label in test_scale.allLabels:
                if test_label == label:
                    return_scales.append(test_label)
        return return_scales

    @staticmethod
    def with_identifier(identifier):
        for test_scale in Scale._scales:
            if test_scale.identifier == identifier:
                return test_scale
        return None

    @staticmethod
    def with_dimensions(dimensions, in_system_of_units=None):
        return_scales = []
        for test_scale in Scale._scales:
            if test_scale.dimensions == dimensions and test_scale.systemOfUnits == in_system_of_units:
                return_scales.append(test_scale)
        return return_scales

    @staticmethod
    def get_ratio_scale(unit, label=None, identifier=None):
        scale = RatioScale(unit, label, identifier)
        scale = Scale._add_when_not_in_cache(scale)
        return scale

    @staticmethod
    def get_interval_scale(base_scale, unit, off_set, label=None, identifier=None):
        scale = IntervalScale(base_scale, unit, off_set, label, identifier)
        scale = Scale._add_when_not_in_cache(scale)
        return scale

    @staticmethod
    def conversion_factor(from_scale, to_scale):
        if (isinstance(from_scale, RatioScale) or isinstance(from_scale, IntervalScale)) and \
                (isinstance(to_scale, RatioScale) or isinstance(to_scale, IntervalScale)):
            can_convert = Unit.can_convert(from_scale.unit, to_scale.unit)
            if not can_convert:
                raise DimensionalException("A scale with dimensions {} cannot be converted to a scale with "
                                           "dimensions {}."
                                           .format(from_scale.dimensions, to_scale.dimensions))
            factor = Unit.conversion_factor(from_scale.unit, to_scale.unit)
            return factor
        else:
            raise ScaleConversionException("Cannot convert from {} to {} as they are not both cardinal scales."
                                            .format(from_scale, to_scale))

    @staticmethod
    def conversion_off_set(from_scale, to_scale):
        if (isinstance(from_scale, RatioScale) or isinstance(from_scale, IntervalScale)) and \
                (isinstance(to_scale, RatioScale) or isinstance(to_scale, IntervalScale)):
            from_ratio_scale = from_scale.base_ratio_scale()
            to_ratio_scale = to_scale.base_ratio_scale()
            from_ratio_factor = Unit.conversion_factor(from_scale.unit,from_ratio_scale[0].unit)
            to_ratio_factor = Unit.conversion_factor(to_scale.unit,to_ratio_scale[0].unit)
            if from_ratio_scale[0] == to_ratio_scale[0]:
                off_set = (to_ratio_scale[1] * to_ratio_factor - from_ratio_scale[1]*from_ratio_factor) \
                          / to_ratio_factor
                return off_set
            else:
                raise ScaleConversionException("Cannot convert from {} to {} as they do not use the same base ratio "
                                               "scale, i.e. they do not have the same known zero point."
                                               .format(from_scale, to_scale))
        else:
            raise ScaleConversionException("Cannot convert from {} to {} as they are not both cardinal scales."
                                            .format(from_scale, to_scale))

    def __init__(self, label=None, identifier=None, dimensions=Dimension(), system_of_units=None):
        super().__init__(label, identifier)
        self.fixedPoints = []
        self.dimensions = dimensions
        self.systemOfUnits = system_of_units

    def add_fixed_point(self, fixed_point):
        self.fixedPoints.append(fixed_point)

    def __str__(self):
        return f'{self.label()}\t<{self.identifier}>  dim: {self.dimensions}'

    def __eq__(self, other):
        if isinstance(other, Scale):
            return str(self.identifier) == str(other.identifier)
        return False

    def __ne__(self, other):
        return not (self == other)


class RatioScale(Scale):

    def __init__(self, unit, label=None, identifier=None, system_of_units=None):
        if not isinstance(unit, Unit):
            raise ValueError("The unit parameter in RatioScale is required to be of type Unit.")
        if system_of_units is None:
            system_of_units = unit.systemOfUnits
        super().__init__(label, identifier, dimensions=unit.dimensions, system_of_units=system_of_units)
        self.unit = unit

    def base_ratio_scale(self):
        return self, 0.0

    def __eq__(self, other):
        if isinstance(other, RatioScale):
            if str(self.identifier) == str(other.identifier):
                return True
            if isinstance(self.identifier, URIRef) and isinstance(other.identifier, URIRef):
                return False
            if self.unit == other.unit:
                return True
        return False

    def __str__(self):
        return f'{self.label()}\t<{self.identifier}> unit: {self.unit} dim: {self.dimensions}'


class IntervalScale(Scale):

    def __init__(self, base_scale, unit, off_set, label=None, identifier=None, system_of_units=None):
        if not isinstance(unit, Unit):
            raise ValueError("The unit parameter in IntervalScale is required to be of type Unit.")
        if not isinstance(base_scale, Scale):
            raise ValueError("The base_scale parameter in IntervalScale is required to be of type Scale.")
        if unit.dimensions != base_scale.dimensions:
            raise DimensionalException("The dimensions of the base scale are not the same as the dimensions of the"
                                       " unit used. {} is not the same as {}"
                                       .format(base_scale.dimensions, unit.dimensions))
        if system_of_units is None:
            system_of_units = base_scale.systemOfUnits
        if system_of_units is None:
            system_of_units = unit.systemOfUnits
        super().__init__(label, identifier, dimensions=unit.dimensions, system_of_units=system_of_units)
        self.unit = unit
        self.baseScale = base_scale
        self.offSet = off_set

    def base_ratio_scale(self):
        base_base = self.baseScale.base_ratio_scale()
        conversion_fac = Unit.conversion_factor(base_base[0].unit, self.unit)
        return base_base[0], base_base[1] * conversion_fac + self.offSet

    def __eq__(self, other):
        if isinstance(other, IntervalScale):
            if str(self.identifier) == str(other.identifier):
                return True
            if self.unit == other.unit and self.baseScale == other.baseScale and self.offSet == other.offSet:
                return True
        return False

    def __str__(self):
        return f'{self.label()}\t<{self.identifier}> base: {self.baseScale} unit: {self.unit} dim: {self.dimensions}'
