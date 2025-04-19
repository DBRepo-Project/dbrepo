
$('.accordion').on('click', function() {

        if ($(this).next('.panel').is(':visible')) {
            $(this).next('.panel').slideUp(
                function() {$(this).prev().css("border-radius", "5px");
            })

        } else {
            $(this).next('.panel').slideDown();
            $(this).css("border-radius", "5px 5px 0 0");
        }

        $(this).find('.icon').toggleClass('right down');
    });

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

function renderTable(dataset, name) {

    const $table = $("#preview_table");

    const $table_name = $("#preview_name");

    $table_name.text(name);


    $table.empty();

    dataset.forEach((rowData, rowIndex) => {
        const $row = $("<tr></tr>");

        Object.values(rowData).forEach(cellData => {
            const $cell = rowIndex === 0 ? $("<th></th>") : $("<td></td>");
            $cell.text(cellData);
            $row.append($cell);
        });

        $table.append($row);
    });
}
