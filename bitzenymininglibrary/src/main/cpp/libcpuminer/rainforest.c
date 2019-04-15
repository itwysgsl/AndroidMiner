#include <miner.h>

#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <stdio.h>

#include "rfv2.h"

#include <stdbool.h>
// struct work_restart {
// 	volatile unsigned long	restart;
// 	char			padding[128 - sizeof(unsigned long)];
// };

extern struct work_restart *work_restart;
extern bool fulltest(const uint32_t *hash, const uint32_t *target);

static int pretest(const uint32_t *hash, const uint32_t *target)
{
	return hash[7] < target[7];
}

int scanhash_rainforest(int thr_id, uint32_t *pdata, const uint32_t *ptarget,
					uint32_t max_nonce, unsigned long *hashes_done)
{
	uint32_t data[20] __attribute__((aligned(64)));
	uint32_t hash[8] __attribute__((aligned(64)));
	uint32_t n = pdata[19];
	const uint32_t first_nonce = pdata[19];
	void *rambox;

	for (int i = 0; i < 20; i++) {
		be32enc(&data[i], pdata[i]);
	}

	rambox = malloc(RFV2_RAMBOX_SIZE * 8);
	if (rambox == NULL)
		goto out;

	rfv2_raminit(rambox);

	do {
		be32enc(&data[19],n);

		rfv2_hash(hash, (char *)data, 80, rambox, NULL);

		if (pretest(hash, ptarget) && fulltest(hash, ptarget)) {
			pdata[19] = n;
			*hashes_done = n - first_nonce + 1;
			return 1;
		}
next:
		n++;
	} while (n < max_nonce && !work_restart[thr_id].restart);
	
	*hashes_done = n - first_nonce + 1;
	pdata[19] = n;
out:
	free(rambox);
	return 0;
}
