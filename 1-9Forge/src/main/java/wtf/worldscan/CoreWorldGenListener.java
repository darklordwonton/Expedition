package wtf.worldscan;

import java.util.HashMap;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wtf.api.PopulationGenerator;
import wtf.api.WTFWorldGen;
import wtf.utilities.wrappers.ChunkCoords;
import wtf.utilities.wrappers.ChunkScan;

public class CoreWorldGenListener {

	public static HashMap<ChunkCoords, ChunkScan> worldChunkScans = new HashMap<ChunkCoords, ChunkScan>();
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void populate(PopulateChunkEvent.Post event) throws Exception{
		
		if (!event.getWorld().isRemote && event.getWorld().getWorldType() != WorldType.FLAT){
			ChunkCoords coords = new ChunkCoords(event.getChunkX(), event.getChunkZ());
			ChunkProviderServer chunkserver = (ChunkProviderServer)event.getWorld().getChunkProvider();

			//Loop that calls on each adjacent chunk to be checked if it should be scanned
			for (int xloop = -1; xloop < 2; xloop++){
				for (int zloop = -1; zloop < 2; zloop++){
					int adjX = coords.getChunkX()+xloop;
					int adjZ = coords.getChunkZ()+zloop;
					if (chunkserver.chunkExists(adjX, adjZ) && event.getWorld().getChunkFromChunkCoords(adjX, adjZ).isTerrainPopulated()){
						
						doScanIfAdjArePopulated(event.getWorld(), chunkserver, event.getRand(), new ChunkCoords(adjX, adjZ));
					
					}
				}	
			}
		}
	}

	public static void doScanIfAdjArePopulated(World world, ChunkProviderServer chunkserver, Random random, ChunkCoords coords) throws Exception{

		//actualy does the checking, and calls the scanning
		for (int xloop = -1; xloop < 2; xloop++){
			for (int zloop = -1; zloop < 2; zloop++){
				int adjX = coords.getChunkX()+xloop;
				int adjZ = coords.getChunkZ()+zloop;
				if (! (chunkserver.chunkExists(adjX, adjZ) && world.getChunkFromChunkCoords(adjX, adjZ).isTerrainPopulated())){
					return;
				}
			}	
		}
		doChunk(world, random, coords);
	}

	//private static int totalTime = 0;
	//private static int count = 0;
	
	public static void doChunk(World world, Random random, ChunkCoords coords) throws Exception{

		WorldScanner scanner = null;//new WorldScanner();
		switch (world.provider.getDimensionType()){
		case NETHER:
			scanner = new NetherScanner();
			break;
		case OVERWORLD:
			scanner = new WorldScanner();
			break;
		case THE_END:
			scanner = new NetherScanner();
			break;
		default:
			break;
	
		}
		
		
		if (scanner != null){
			//long pre = System.nanoTime();
			
			ChunkScan scan = scanner.getChunkScan(world, coords);
			
			//long post = System.nanoTime();
			//totalTime += (post-pre)/100000;
			//count++;
			//int average = totalTime/count;
			//System.out.println("Average scan time = " + average);
			


			worldChunkScans.put(coords, scan);


			for (PopulationGenerator generator : WTFWorldGen.getGenerators()){
				generator.generate(world, coords, random, scan);
			}

			//long endTime = System.currentTimeMillis();
			//System.out.println("population "+ event.getWorld().getBiomeGenForCoords(new BlockPos(coords.getWorldX(), 100, coords.getWorldZ())).getBiomeName() + (endTime - startTime) + " milliseconds");

		}

	}



	public static ChunkScan getChunkScan(World world, ChunkCoords coords) throws Exception{

		ChunkScan scan = worldChunkScans.get(coords);

		if (scan == null && coords.exists(world)){
			//System.out.println("Scan not found for existing chunk " + coords.getChunkX() + " " + coords.getChunkZ() + " ");
			//System.out.println("chunk populated " + coords.getChunk(world).isPopulated() + " terrain populated " + coords.getChunk(world).isTerrainPopulated());
			WorldScanner scanner = new WorldScanner();
			scan = scanner.getChunkScan(world, coords);

			worldChunkScans.put(coords, scan);
		}
		else{
			//System.out.println("Chunk not loaded");
		}

		if (worldChunkScans.size() > 1000){
			System.out.println("Clearing World Scans");
			worldChunkScans.clear();
		}
		return scan;
	}



}
