#!/usr/bin/env python3.11

import aiohttp
import asyncio

async def main():

    async with aiohttp.ClientSession() as session:
        for _ in range(100):
            await session.get(f"http://localhost:8000/simulate?generations={7500}&world={4}&scenario={3}")

asyncio.run(main())
