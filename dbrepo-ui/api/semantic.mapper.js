class SemanticMapper {
  mapConcepts (concepts) {
    return concepts.map((concept) => {
      concept.name = concept.name ? concept.name : concept.uri
      return concept
    })
  }

  mapUnits (units) {
    return units.map((unit) => {
      unit.name = unit.name ? unit.name : unit.uri
      return unit
    })
  }
}

export default new SemanticMapper()
