
$('.preview_button').on('click', async function() {
    const database_id = $(this).data('database_id');
    const preview_id = $(this).data('value');
    const name = $(this).data('name');

    const response = await fetch(`/get-data?database_id=${database_id}&id=${preview_id}`);
    if (!response.ok) {
        throw new Error("Failed to fetch data");
    }

    const dataset = await response.json();

    renderTable(dataset, name);
});

$('.copy-doi-button').on('click', async function() {
        const value = $(this).data('value');

        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(value);
        } else {
            // Fallback method
            const temp = document.createElement('textarea');
            temp.value = value;
            document.body.appendChild(temp);
            temp.select();
            document.execCommand('copy');
            document.body.removeChild(temp);
        }
});

$('.subset_preview_button').on('click', async function() {
    const database_id = $(this).data('database_id');
    const preview_id = $(this).data('value');
    const name = $(this).data('name');


    const response = await fetch(`/get-subset-data?database_id=${database_id}&id=${preview_id}`);
    if (!response.ok) {
        throw new Error("Failed to fetch data");
    }
    const dataset = await response.json();

    renderTable(dataset, name);
});

$('.subset_pagination_button').on('click', async function() {
    const database_id = $(this).data('database_id');
    const preview_id = $(this).data('value');

    const response = await fetch(`/get-subset-data?database_id=${database_id}&id=${preview_id}`);
    if (!response.ok) {
        throw new Error("Failed to fetch data");
    }
    const dataset = await response.json();

    renderTable(dataset, "name");
});

function renderTable(dataset, name) {
    const $table = $("#preview_table");

    const $table_name = $("#preview_name");

    $table_name.text(name);
    $table.empty();

    if (!dataset || dataset.length === 0) return;

    const $thead = $("<thead></thead>");
    const $tbody = $("<tbody></tbody>");

    // ----- Header from keys (like Jinja) -----
    const $headerRow = $("<tr></tr>");
    Object.keys(dataset[0]).forEach(column => {
        $("<th></th>").text(column).appendTo($headerRow);
    });
    $thead.append($headerRow);

    // ----- Body from values -----
    dataset.forEach(rowData => {
        const $row = $("<tr></tr>");
        Object.values(rowData).forEach(value => {
            $("<td></td>").text(value).appendTo($row);
        });
        $tbody.append($row);
    });

    $table.append($thead, $tbody);
}
