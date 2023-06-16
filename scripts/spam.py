#!/usr/bin/env python3.11

import aiohttp
import asyncio

async def main():

    async with aiohttp.ClientSession() as session:
        for _ in range(1):
            session.get(f"http://localhost:8000/simulate?generations={100000}&world={4}&scenario={3}")
            print("Request sent!")

asyncio.run(main())
