from dbrepo.core.omlib.exceptions.dimensionexception import DimensionalException


class Dimension:

    def __init__(self, T=0, L=0, M=0, I=0, Theta=0, N=0, J=0):
        self.T = T
        self.L = L
        self.M = M
        self.I = I
        self.Theta = Theta
        self.N = N
        self.J = J

    def time_dimension_exponent(self):
        return self.T

    def length_dimension_exponent(self):
        return self.L

    def mass_dimension_exponent(self):
        return self.M

    def electric_current_dimension_exponent(self):
        return self.I

    def thermodynamic_temperature_dimension_exponent(self):
        return self.Theta

    def amount_of_substance_dimension_exponent(self):
        return self.N

    def luminous_intensity_dimension_exponent(self):
        return self.J

    def __eq__(self, other):
        return (self.T == other.T and self.L == other.L and self.M == other.M and self.I == other.I and
                self.Theta == other.Theta and self.N == other.N and self.J == other.J)

    def __str__(self):
        return f'(T={self.T}, L={self.L}, M={self.M}, I={self.I}, θ={self.Theta}, N={self.N}, J={self.J})'

    def __add__(self, other):
        if not self == other:
            raise DimensionalException("Entities of different dimensions cannot be added together. {} != {}"
                                       .format(self, other))
        return self

    def __sub__(self, other):
        if not self == other:
            raise DimensionalException("Entities of different dimensions cannot be subtracted from each other. {} != {}"
                                       .format(self, other))
        return self

    def __mul__(self, other):
        return Dimension(self.T + other.T, self.L + other.L, self.M + other.M, self.I + other.I,
                         self.Theta + other.Theta, self.N + other.N, self.J + other.J)

    def __truediv__(self, other):
        return Dimension(self.T - other.T, self.L - other.L, self.M - other.M, self.I - other.I,
                         self.Theta - other.Theta, self.N - other.N, self.J - other.J)

    @staticmethod
    def pow(base, exponent):
        if isinstance(base, Dimension):
            return Dimension(base.T * exponent, base.L * exponent, base.M * exponent, base.I * exponent,
                             base.Theta * exponent, base.N * exponent, base.J * exponent)
        else:
            return Dimension()
