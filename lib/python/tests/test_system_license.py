import unittest

from dbrepo.RestClient import RestClient

from dbrepo.api.dto import License


class LicenseSystemTest(unittest.TestCase):

    def test_get_licenses_succeeds(self):
        exp = [License(identifier='CC-BY-4.0', uri='https://creativecommons.org/licenses/by/4.0/legalcode',
                       description='The Creative Commons Attribution license allows re-distribution and re-use of a licensed work on the condition that the creator is appropriately credited.'),
               License(identifier='CC0-1.0', uri='https://creativecommons.org/publicdomain/zero/1.0/legalcode',
                       description='CC0 waives copyright interest in a work you\'ve created and dedicates it to the world-wide public domain. Use CC0 to opt out of copyright entirely and ensure your work has the widest reach.')]
        # test
        response = RestClient().get_licenses()
        self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()
