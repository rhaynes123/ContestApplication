const form = document.getElementById('contest-form');
const list = document.getElementById('contest-list');
const empty = document.getElementById('empty-state');
const errorEl = document.getElementById('form-error');
const submitBtn = document.getElementById('submit-btn');

function statusBadge(c) {
    const deadline = new Date(c.deadline).getTime();
    if (deadline > Date.now()) {
        return { text: 'Open · closes ' + new Date(c.deadline).toLocaleString(), cls: 'open' };
    }
    return c.isWon
        ? { text: 'Closed · won', cls: 'won' }
        : { text: 'Closed · unclaimed', cls: 'unclaimed' };
}

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

        const a = document.createElement('a');
        a.className = 'contest-link';
        a.href = 'contest.html?id=' + encodeURIComponent(c.id);

        const name = document.createElement('p');
        name.className = 'contest-name';
        name.textContent = c.name;
        a.appendChild(name);

        const meta = document.createElement('div');
        meta.className = 'contest-meta';
        const badge = statusBadge(c);

        const badgeEl = document.createElement('span');
        badgeEl.className = 'badge ' + badge.cls;
        badgeEl.textContent = badge.text;
        meta.appendChild(badgeEl);
        a.appendChild(meta);

        const prizes = document.createElement('ul');
        prizes.className = 'prize-list';
        for (const p of c.prizes ?? []) {
            prizes.appendChild(prizeItem(p));
        }
        a.appendChild(prizes);
        li.appendChild(a);
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
        const data = new FormData(form);
        const localDeadline = data.get('deadline');
        if (localDeadline) {
            data.set('deadline', new Date(localDeadline).toISOString());
        }
        const res = await fetch('/contests', {
            method: 'POST',
            body: new URLSearchParams(data),
        });
        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            throw new Error(body.error ?? ('Server returned ' + res.status));
        }
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
