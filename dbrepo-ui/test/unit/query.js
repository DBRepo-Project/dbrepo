const test = require('ava')
const { buildQuery, castNum } = require('@/server-middleware/query')

test('buildQuery_succeeds', (t) => {
  const r = buildQuery({
    table: 'Table'
  })
  t.is(r.sql, 'select * from `Table`')
})

test('buildQuery_columns_succeeds', (t) => {
  const r = buildQuery({
    table: 'Table',
    select: ['database', 'bbb']
  })
  t.is(r.sql, 'select `database`, `bbb` from `Table`')
})

test('buildQuery_where_succeeds', (t) => {
  const r = buildQuery({
    table: 'Table',
    clauses: [
      { type: 'where', params: ['foo', '=', 42] }
    ]
  })
  t.is(r.sql, 'select * from `Table` where `foo` = 42')
})

test('buildQuery_whereNumeric_succeeds', (t) => {
  const r = buildQuery({
    table: 'Table',
    clauses: [
      { type: 'where', params: ['foo', '=', '42'] }
    ]
  })
  t.is(r.sql, 'select * from `Table` where `foo` = 42')
})

test('buildQuery_whereString_succeeds', (t) => {
  const r = buildQuery({
    table: 'Table',
    clauses: [
      { type: 'where', params: ['foo', '=', 'bla'] }
    ]
  })
  t.is(r.sql, 'select * from `Table` where `foo` = \'bla\'')
})

test('buildQuery_illegalOperator_fails', (t) => {
  const r = buildQuery({
    table: 'Table',
    clauses: [
      { type: 'where', params: ['foo', 'UNKNOWN', 42] }
    ]
  })
  t.is(r.sql, undefined)
  t.is(r.error, 'The operator "UNKNOWN" is not permitted')
})

test('buildQuery_whereAndExplicit_succeeds', (t) => {
  const r = buildQuery({
    table: 'Table',
    clauses: [
      { type: 'where', params: ['foo', '=', 42] },
      { type: 'and' }, // here, unlike below
      { type: 'where', params: ['bar', '=', 42] }
    ]
  })
  t.is(r.sql, 'select * from `Table` where `foo` = 42 and `bar` = 42')
})

test('buildQuery_whereAndImplicit_succeeds', (t) => {
  const r = buildQuery({
    table: 'Table',
    clauses: [
      { type: 'where', params: ['foo', '=', 42] },
      // not here, unlike above
      { type: 'where', params: ['bar', '=', 42] }
    ]
  })
  t.is(r.sql, 'select * from `Table` where `foo` = 42 and `bar` = 42')
})

test('buildQuery_whereOr', (t) => {
  const r = buildQuery({
    table: 'Table',
    clauses: [
      { type: 'where', params: ['foo', '=', 42] },
      { type: 'or' },
      { type: 'where', params: ['bar', '=', 42] }
    ]
  })
  t.is(r.sql, 'select * from `Table` where `foo` = 42 or `bar` = 42')
})

test('castNum_succeeds', (t) => {
  t.is(castNum(''), '')
  t.is(castNum(' '), ' ')
  t.is(castNum('0'), 0)
  t.is(castNum('0 '), '0 ')
  t.is(castNum('1'), 1)
  t.is(castNum('1 '), '1 ')
  t.is(castNum(1), 1)
  t.is(castNum('1'), 1)
  t.is(castNum('1.1'), 1.1)
  t.is(castNum('69.420'), '69.420')
  t.is(castNum('a'), 'a')
})
