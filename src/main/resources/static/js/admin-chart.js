const batang = document.querySelectorAll("canvas[data-jumlah]");

if (batang.length > 0) {
    const maksimum = Math.max(1, Number(batang[0].dataset.jumlah));
    for (const kanvas of batang) {
        const lebar = kanvas.width * Number(kanvas.dataset.jumlah) / maksimum;
        kanvas.getContext("2d").fillRect(0, 0, lebar, kanvas.height);
    }
}