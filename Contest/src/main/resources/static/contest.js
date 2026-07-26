const params = new URLSearchParams(window.location.search);
const id = params.get('id');

const nameEl = document.getElementById('contest-name');
const metaEl = document.getElementById('contest-meta');
const guessCard = document.getElementById('guess-card');
const guessForm = document.getElementById('guess-form');
const guessSubmit = document.getElementById('guess-submit');
const guessError = document.getElementById('guess-error');
const guessOk = document.getElementById('guess-ok');
const revealEl = document.getElementById('reveal');
const prizeList = document.getElementById('prize-list');

const PLACE_LABEL = { FIRST: '1st', SECOND: '2nd', THIRD: '3rd' };
const PLACE_CLASS = { FIRST: 'first', SECOND: 'second', THIRD: 'third' };

function renderContest(c) {
    nameEl.textContent = c.name;
    const deadline = new Date(c.deadline);
    const open = deadline.getTime() > Date.now();
    metaEl.textContent = open
        ? 'Open · closes ' + deadline.toLocaleString()
        : 'Closed at ' + deadline.toLocaleString() + (c.isWon ? ' · all prizes won' : ' · not all prizes claimed');

    guessCard.hidden = !open;

    if (c.secretValue != null) {
        revealEl.hidden = false;
        revealEl.textContent = 'Secret was: ' + c.secretValue;
    } else {
        revealEl.hidden = true;
    }

    prizeList.innerHTML = '';
    for (const p of c.prizes ?? []) {
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
        } else if (!open) {
            const w = document.createElement('span');
            w.className = 'empty';
            w.textContent = '(unclaimed)';
            item.appendChild(w);
        }
        prizeList.appendChild(item);
    }
}

async function load() {
    const res = await fetch('/contests/' + encodeURIComponent(id));
    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        nameEl.textContent = body.error === 'not_found' ? 'Contest not found' : 'Error';
        return;
    }
    renderContest(await res.json());
}

guessForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    guessError.hidden = true;
    guessOk.hidden = true;
    guessSubmit.disabled = true;
    try {
        const res = await fetch('/contests/' + encodeURIComponent(id) + '/guesses', {
            method: 'POST',
            body: new URLSearchParams(new FormData(guessForm)),
        });
        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            throw new Error(body.error ?? ('Server returned ' + res.status));
        }
        guessForm.reset();
        guessOk.hidden = false;
    } catch (err) {
        guessError.textContent = err.message;
        guessError.hidden = false;
    } finally {
        guessSubmit.disabled = false;
    }
});

if (!id) {
    nameEl.textContent = 'Missing contest id';
} else {
    load();
}
