import { test, expect } from '@playwright/test';

const apiBase = /http:\/\/localhost:8080\/api/;

test('empty state and import flow', async ({ page }) => {
  await page.route(`${apiBase}characters`, async (route) => {
    await route.fulfill({ status: 200, body: JSON.stringify([]) });
  });

  await page.route(`${apiBase}characters/import*`, async (route) => {
    await route.fulfill({
      status: 201,
      body: JSON.stringify({
        id: '1',
        name: 'Jan Paweł II',
        birthYear: 1920,
        deathYear: 2005,
        biography: 'Papież',
        imageUrl: null,
        era: 'XX wiek',
        nationality: 'Polska',
        createdAt: '',
        updatedAt: '',
      }),
    });
  });

  await page.goto('/');
  await expect(page.getByText('Brak postaci w bazie danych')).toBeVisible();

  await page.getByPlaceholder('Wyszukaj postać historyczną...').fill('Jan Paweł II');
  await page.getByRole('button', { name: /Dodaj z Wikipedia/i }).click();

  await expect(page.getByText('Zaimportowano pomyślnie!')).toBeVisible();
});

test('error state shows retry', async ({ page }) => {
  await page.route(`${apiBase}characters`, async (route) => {
    await route.fulfill({ status: 500, body: JSON.stringify({ message: 'boom' }) });
  });

  await page.goto('/');
  await expect(page.getByText('Nie udało połączyć się z serwerem')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Spróbuj ponownie' })).toBeVisible();
});

test('renders character list', async ({ page }) => {
  await page.route(`${apiBase}characters`, async (route) => {
    await route.fulfill({
      status: 200,
      body: JSON.stringify([
        {
          id: '1',
          name: 'Mikołaj Kopernik',
          birthYear: 1473,
          deathYear: 1543,
          biography: 'Astronom',
          imageUrl: null,
          era: 'Renesans',
          nationality: 'Polska',
          createdAt: '',
          updatedAt: '',
        },
      ]),
    });
  });

  await page.goto('/');
  await expect(page.getByText('Mikołaj Kopernik')).toBeVisible();
  await expect(page.getByText('Renesans')).toBeVisible();
  await expect(page.getByText('Polska')).toBeVisible();
});
