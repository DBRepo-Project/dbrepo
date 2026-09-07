package at.ac.tuwien.ifs.dbrepo.service;

public record DatabaseSynchronisationResult(int tables, int pages, int tuples, int replicaWrites) {
}
