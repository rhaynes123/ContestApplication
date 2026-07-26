const PLACE_LABEL = { FIRST: '1st', SECOND: '2nd', THIRD: '3rd' };
const PLACE_CLASS = { FIRST: 'first', SECOND: 'second', THIRD: 'third' };

function prizeItem(p, { showUnclaimed = false } = {}) {
    const item = document.createElement('li');
    item.className = 'prize';
    const place = document.createElement('span');
    place.className = 'place ' + (PLACE_CLASS[p.place] ?? '');
    place.textContent = PLACE_LABEL[p.place] ?? p.place;
    const value = document.createElement('span');
    value.textContent = p.value;
    item.append(place, value);
    if (p.winnerName) {
        const w = document.createElement('span');
        w.className = 'winner';
        w.textContent = '→ ' + p.winnerName;
        item.appendChild(w);
    } else if (showUnclaimed) {
        const w = document.createElement('span');
        w.className = 'empty';
        w.textContent = '(unclaimed)';
        item.appendChild(w);
    }
    return item;
}
