import http from "k6/http";
import { sleep } from "k6";

export const options = {
    vus: 28,          // 7 rooms × 4 jugadores = 28 hilos simulados
    duration: "35s",  // duración total
};

const BACKEND = "https://vampiremultiplesurvivors-h3gfb9gsf4bscre2.canadacentral-01.azurewebsites.net";

// 7 rooms simuladas
const ROOMS = [
    "ROOM01",
    "ROOM02",
    "ROOM03",
    "ROOM04",
    "ROOM05",
    "ROOM06",
    "ROOM07"
];

// Cada 4 usuarios, una room
function getRoomForVu(vu) {
    const index = Math.floor((vu - 1) / 4); // 0–6
    return ROOMS[index];
}

// Jugadores dentro de cada room → Player1, Player2, Player3, Player4
function getPlayerName(vu) {
    const numInRoom = ((vu - 1) % 4) + 1; // 1–4
    return "Player" + numInRoom;
}

export default function () {
    const vu = __VU;

    const room = getRoomForVu(vu);
    const player = getPlayerName(vu);

    const url = `${BACKEND}/api/players/${room}/move?playerName=${player}&arriba=false&abajo=false&izquierda=false&derecha=true`;

    const res = http.put(url, null, {
        headers: {
            "X-MS-CLIENT-PRINCIPAL-ID": "dev-user"
        }
    });

    console.log(
        `Room ${room} → ${player} → Latencia: ${res.timings.duration} ms (VU ${vu})`
    );

    sleep(0.3);
}
