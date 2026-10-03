const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const ui = path.resolve(__dirname, '../dbrepo-ui')
const fromUi = createRequire(path.join(ui, 'package.json'))
const { parse } = fromUi('@vue/compiler-sfc')
const { parse: parseJavaScript } = fromUi('@babel/parser')

function options(file) {
  const { descriptor, errors } = parse(fs.readFileSync(path.join(ui, file), 'utf8'))
  assert.deepEqual(errors, [], file)
  const script = descriptor.script.content
  const ast = parseJavaScript(script, { sourceType: 'module' })
  const imports = ast.program.body.filter(node => node.type === 'ImportDeclaration')
    .flatMap(node => node.specifiers.map(specifier => specifier.local.name))
  const exported = ast.program.body.find(node => node.type === 'ExportDefaultDeclaration').declaration
  return new Function(...imports, `return (${script.slice(exported.start, exported.end)})`)(...imports.map(() => ({})))
}

const context = {
  table: { owner: { username: 'alice' }, archived_at: '2026-10-03T10:00:00Z', identifiers: [] },
  database: { owner: { username: 'alice' } },
  cacheUser: { preferred_username: 'alice' },
  access: { type: 'write_all' },
  roles: ['system', 'update-table', 'insert-table-data', 'update-table-data', 'delete-table-data',
    'delete-table', 'delete-foreign-table', 'create-database-view', 'modify-foreign-table-column-semantics'],
  selection: [{}], isOwner: true, hasReadAccess: true, secondaryReplica: false
}

for (const [file, permissions] of [
  ['components/table/TableToolbar.vue', ['canUpdateTable', 'canCreateView', 'canImportCsv', 'canGetPid']],
  ['pages/database/[database_id]/table/[table_id]/schema.vue', ['canAssignSemanticInformation']]
]) {
  const component = options(file)
  for (const permission of permissions) {
    assert.equal(component.computed[permission].call(context), false, `${file}: ${permission} on archived table`)
    assert.equal(component.computed[permission].call({ ...context, table: { ...context.table, archived_at: null } }),
      true, `${file}: ${permission} on active primary table`)
  }
}

assert.equal(options('components/ResourceStatus.vue').computed.mode.call({ resource: context.table }), 'archived')
console.log('Replica and archive UI permission checks passed')
