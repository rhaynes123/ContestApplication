const form = document.getElementById('contest-form');
    const list = document.getElementById('contest-list');
    const empty = document.getElementById('empty-state');
    const errorEl = document.getElementById('form-error');
    const submitBtn = document.getElementById('submit-btn');

    const PLACE_LABEL = { FIRST: '1st', SECOND: '2nd', THIRD: '3rd' };
    const PLACE_CLASS = { FIRST: 'first', SECOND: 'second', THIRD: 'third' };

    function render(contests) {
        list.innerHTML = '';
        if (!contests.length) {
            empty.hidden = false;
            return;
        }
        empty.hidden = true;
        for (const c of contests) {
            const li = document.createElement('li');
            li.className = 'contest';

            const name = document.createElement('p');
            name.className = 'contest-name';
            name.textContent = c.name;
            li.appendChild(name);

            const prizes = document.createElement('ul');
            prizes.className = 'prize-list';
            for (const p of c.prizes ?? []) {
                const item = document.createElement('li');
                item.className = 'prize';
                const place = document.createElement('span');
                place.className = 'place ' + (PLACE_CLASS[p.place] ?? '');
                place.textContent = PLACE_LABEL[p.place] ?? p.place;
                const value = document.createElement('span');
                value.textContent = p.value;
                item.append(place, value);
                prizes.appendChild(item);
            }
            li.appendChild(prizes);
            list.appendChild(li);
        }
    }

    async function loadContests() {
        const res = await fetch('/contests');
        if (!res.ok) throw new Error('Failed to load contests');
        render(await res.json());
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorEl.hidden = true;
        submitBtn.disabled = true;
        try {
            const res = await fetch('/contests', {
                method: 'POST',
                body: new URLSearchParams(new FormData(form)),
            });
            if (!res.ok) throw new Error('Server returned ' + res.status);
            form.reset();
            await loadContests();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.hidden = false;
        } finally {
            submitBtn.disabled = false;
        }
    });

    loadContests().catch((err) => {
        errorEl.textContent = err.message;
        errorEl.hidden = false;
    });