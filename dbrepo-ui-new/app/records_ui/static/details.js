
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

$('.preview_button').on('click', function() {
    const preview_id = $(this).data('value');
    renderTable(preview_id);
});

async function renderTable(index) {

    console.log(index)
    const response = await fetch(`/get-data?id=${index}`);
    if (!response.ok) {
        throw new Error("Failed to fetch data");
    }

    const dataset = await response.json();
    console.log(dataset);


    const $table = $("#preview_table");

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
