interface Dataset {
  '@context': string;
  '@type': string;
  name: string | null;
  description: string | null;
  url: string;
  isAccessibleForFree: boolean;
  creator: Person;
}

interface Person {
  '@type': string;
  name: string | null;
  givenName: string | null;
  familyName: string | null;
  sameAs: string | null;
}
