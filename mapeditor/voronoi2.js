function voro() {
  let col1 = "#ffcc00";
  let col2 = "#aaaaaa";
  let col3 = "#0088ff";
  let Nmw = 0;
  let takamw = 0;
  let habamw = 0;
  let kmw = 0;
  let imw = 0;
  let jmw = 0;
  let w1mw1 = 0;
  let w2mw2 = 0;
  let xmw1 = 0;
  let ymw1 = 0;
  let r1mw = 0;
  let r2mw = 0;
  let rmw = 0;
  let x2mwI = 0;
  let x2mws = 0;
  let x2mwe = 0;
  let y2mwI = 0;
  let y2mws = 0;
  let y2mwe = 0;
  let y3mwI = 0;
  let x3mwI = 0;
  let y1mw1 = 0;
  let y2mw2 = 0;
  let d2mw = 0;
  let d3mw = 0;
  let y3mw3 = 0;
  let d4mw = 0;
  let d5mw = 0;
  let x1mw1 = 0;
  let x2mw2 = 0;
  let d6mw = 0;
  let d7mw = 0;
  let x3mw3 = 0;
  let d8mw = 0;
  let d9mw = 0;
  let br1mw=0 = 0;
  let br2mw=0 = 0;
  let br3mw=0 = 0;
  let br4mw=0 = 0;
  let Nd = 0;
  let height = 512;
  let width = 512;
	public void init(){
			takaS=getParameter("takap");//get height of screen from HTML file 高さをHTMLファイルから取得する
			habaS=getParameter("habap");//get width of screen from HTML file 幅をHTMLファイルから取得する
			NS=getParameter("Np");//get number of generators from HTML file 点の数をHTMLファイルから取得する
			width=dou(habaS);
			height=dou(takaS);
			Nd=dou(NS);
			if(Nd==6){
					Nd=4+16*Math.random();//number of generators are defined randomly 乱数で点の数を決める
			}
			habamw=(int)width;//finally width of screen is defined 最終的な画面の幅
			takamw=(int)height;//finally height of screen is defined 最終的な画面の高さ
			Nmw=(int)Nd;//finally number of generators is defined 最終的な点の数
	}
	double x1mw[]=new double[100];
	double y1mw[]=new double[100];
	double w1mw[]=new double[100];
	int xmw[]=new int[100];
	int ymw[]=new int[100];
	int wmw[]=new int[100];
	double smw[]=new double[100];
	String sssmw[]=new String[100];
	public double joumw(double amw,double bmw){
			double jou1mw;
			jou1mw=Math.pow(amw,bmw);
			return jou1mw;
	}

	//sort he1aw[0]<he1aw[1]<he1aw[2]<...<he1aw[NNaw-1]
	void heapmw(double he1mw[],double he2mw[],double he3mw[],int NNmw){
			int kkmw,kksmw,iimw,jjmw,mmmw;
			double b1mw,b2mw,b3mw,c1mw,c2mw,c3mw;
			kksmw=(int)(NNmw/2);
			for(kkmw=kksmw;kkmw>=1;kkmw--){
					iimw=kkmw;
					b1mw=he1mw[iimw-1];b2mw=he2mw[iimw-1];b3mw=he3mw[iimw-1];
					while(2*iimw<=NNmw){
							jjmw=2*iimw;
							if(jjmw+1<=NNmw){
									if(he1mw[jjmw-1]<he1mw[jjmw]){
											jjmw++;
									}
							}
							if(he1mw[jjmw-1]<=b1mw){
									break;
							}
							he1mw[iimw-1]=he1mw[jjmw-1];he2mw[iimw-1]=he2mw[jjmw-1];he3mw[iimw-1]=he3mw[jjmw-1];
							iimw=jjmw;
					}//wend
					he1mw[iimw-1]=b1mw;he2mw[iimw-1]=b2mw;he3mw[iimw-1]=b3mw;
			}//next kk
			for(mmmw=NNmw-1;mmmw>=1;mmmw--){
					c1mw=he1mw[mmmw];c2mw=he2mw[mmmw];c3mw=he3mw[mmmw];
					he1mw[mmmw]=he1mw[0];he2mw[mmmw]=he2mw[0];he3mw[mmmw]=he3mw[0];
					iimw=1;
					while(2*iimw<=mmmw){
							kkmw=2*iimw;
							if(kkmw+1<=mmmw){
									if(he1mw[kkmw-1]<=he1mw[kkmw]){
											kkmw++;
									}
							}
							if(he1mw[kkmw-1]<=c1mw){
									break;
							}
							he1mw[iimw-1]=he1mw[kkmw-1];he2mw[iimw-1]=he2mw[kkmw-1];he3mw[iimw-1]=he3mw[kkmw-1];
							iimw=kkmw;
					}//wend
					he1mw[iimw-1]=c1mw;he2mw[iimw-1]=c2mw;he3mw[iimw-1]=c3mw;
			}//next mm
	}



	public void paint(java.awt.Graphics g){
			g.setColor(col1);
			g.fillRect(1,1,habamw,takamw);
			g.setColor(col2);
			g.drawString("N="+Nmw,15,15);

			//set Naw generators (x1aw[kaw],[y1aw[kaw]) with weight w1aw[kaw] rondomly.　母点の座標を乱数で決める
			for(kmw=0;kmw<Nmw;kmw++){
					x1mw[kmw]=Math.random()*(habamw-30)+15;//x coordinate  x座標
					y1mw[kmw]=Math.random()*(takamw-30)+15;//y coordinate  y座標
					w1mw[kmw]=Math.random()*100+1;//weight 重み
					xmw[kmw]=(int)(x1mw[kmw]+0.5);
					ymw[kmw]=(int)(y1mw[kmw]+0.5);
					wmw[kmw]=(int)(w1mw[kmw]+0.5);
					sssmw[kmw]=""+wmw[kmw];
					g.drawString(sssmw[kmw],xmw[kmw]-3,ymw[kmw]-3);
					g.fillOval(xmw[kmw]-2,ymw[kmw]-2,4,4);
			}


			//sort Nmw generators so that w1aw[0]<w1aw[1]<...<w1aw[Nmw-1]　重みの小さい方が先になるように並びかえる
			heapmw(w1mw,x1mw,y1mw,Nmw);
			g.setColor(col3);

			//consider the bisector between i and j　iとjの境界を考えます
			//Regarding to multiplicatively weighted Voronoi,　ＭＷボロノイ図（乗法的重み付きボロノイ図）の場合、
			//bisectors are Apporonius' circle;　境界線はアポロニウスの園になります。
			//  they mean that points are the rate of distance from i and distance from j is same.　すなわち、iからの距離とjからの距離の比が等しい点の軌跡になります。
			//  See Okabe, Boots, Sugihara and Chiu. Spatial Tessellations, Wiley. を参照
			for(imw=1;imw<=Nmw-1;imw++){
					for(jmw=imw+1;jmw<=Nmw;jmw++){
							w1mw1=joumw(w1mw[jmw-1],2)/(joumw(w1mw[jmw-1],2)-joumw(w1mw[imw-1],2));
							w2mw2=joumw(w1mw[imw-1],2)/(joumw(w1mw[jmw-1],2)-joumw(w1mw[imw-1],2));
							xmw1=w1mw1*x1mw[imw-1]-w2mw2*x1mw[jmw-1];
							ymw1=w1mw1*y1mw[imw-1]-w2mw2*y1mw[jmw-1];
							r1mw=w1mw[imw-1]*w1mw[jmw-1]/(joumw(w1mw[jmw-1],2)-joumw(w1mw[imw-1],2));
							r2mw=joumw(joumw(x1mw[imw-1]-x1mw[jmw-1],2)+joumw(y1mw[imw-1]-y1mw[jmw-1],2),0.5);
							rmw=r1mw*r2mw;
							x2mws=(int)(xmw1-rmw+0.5);
							x2mwe=(int)(xmw1+rmw+0.5);

							//Consider the circle　円上の点に対して計算する
							//Loop x= left edge of the circle to right edge. x=円の左端から右端まで
							for(x2mwI=x2mws;x2mwI<=x2mwe;x2mwI++){
									if(x2mwI>=0 && x2mwI<=habamw){
											// compute upper y coordinate for x2mwI yが円の上半分の場合
											y1mw1=rmw*rmw-joumw(x2mwI-xmw1,2);

											if(y1mw1<0){
													y1mw1=0;
											}

											y2mw2=ymw1+joumw(y1mw1,0.5);

											//compute distance from i to (x2mwI,y2mw2)　iからの距離を計算
											d2mw=joumw(joumw(x2mwI-x1mw[imw-1],2)+joumw(y2mw2-y1mw[imw-1],2),0.5)/w1mw[imw-1];
											//loop k (not i, not j)　i,j以外のkに対して
											for(kmw=1;kmw<=Nmw;kmw++){
													if(kmw!=imw && kmw!=jmw){
															//compute distance from k to (x2mwI,y2mw2)　kまでの距離を計算
															d3mw=joumw(joumw(x2mwI-x1mw[kmw-1],2)+joumw(y2mw2-y1mw[kmw-1],2),0.5)/w1mw[kmw-1];
															if(d2mw>d3mw){//it means k is near than i (or j), so we cannot plot this (x,y)　kまでの距離の方がiまでの距離よりも小さければ、この境界線はボロノイ境界線にはならない
																	br1mw=1;
																	break;
															}
													}//if kmw!=imw...
											}//next kmw
											if(br1mw==0){//this means i is the nearest points from (x,y), so we can plot (x,y)　iよりも近いkがない場合、境界線はボロノイ境界線になる
													y2mwI=(int)(y2mw2+0.5);
													g.drawLine(x2mwI,y2mwI,x2mwI,y2mwI);
											}//if br1==0
											else{//br1mw==1
													br1mw=0;
											}

											// compute lower y coordinate for x2mwI　円の下側について考える
											//  do same thing of upper y.　考え方は上側のときと同じ
											y3mw3=ymw1-joumw(y1mw1,0.5);
											d4mw=joumw(joumw(x2mwI-x1mw[imw-1],2)+joumw(y3mw3-y1mw[imw-1],2),0.5)/w1mw[imw-1];
											for(kmw=1;kmw<=Nmw;kmw++){
													if(kmw!=imw && kmw!=jmw){
															d5mw=joumw(joumw(x2mwI-x1mw[kmw-1],2)+joumw(y3mw3-y1mw[kmw-1],2),0.5)/w1mw[kmw-1];
															if(d4mw>d5mw){
																	br2mw=1;
																	break;
															}
													}//if kmw!=imw...
											}//next kmw
											if(br2mw==0){
													y3mwI=(int)(y3mw3+0.5);
													g.drawLine(x2mwI,y3mwI,x2mwI,y3mwI);
											}//if br1==0
											else{//br1mw==1
													br2mw=0;
											}
									}//if x2mwI>=0....
							}// next x2mwI

							//consider same circle for y-loop.　円の軌跡をy方向についても行う
							// do same thing of x-loop.　考え方はｘ方向のときと同じ
							y2mws=(int)(ymw1-rmw+0.5);
							y2mwe=(int)(ymw1+rmw+0.5);
							for(y2mwI=y2mws;y2mwI<=y2mwe;y2mwI++){
									if(y2mwI>=0 && y2mwI<=takamw){
											x1mw1=rmw*rmw-joumw(y2mwI-ymw1,2);
											if(x1mw1<0){
													x1mw1=0;
											}
											x2mw2=xmw1+joumw(x1mw1,0.5);
											d6mw=joumw(joumw(x2mw2-x1mw[imw-1],2)+joumw(y2mwI-y1mw[imw-1],2),0.5)/w1mw[imw-1];
											for(kmw=1;kmw<=Nmw;kmw++){
													if(kmw!=imw && kmw!=jmw){
															d7mw=joumw(joumw(x2mw2-x1mw[kmw-1],2)+joumw(y2mwI-y1mw[kmw-1],2),0.5)/w1mw[kmw-1];
															if(d6mw>d7mw){
																	br3mw=1;
																	break;
															}
													}//if kmw!=imw...
											}//next kmw
											if(br3mw==0){
													x2mwI=(int)(x2mw2+0.5);
													g.drawLine(x2mwI,y2mwI,x2mwI,y2mwI);
											}//if br1==0
											else{//br1mw==1
													br3mw=0;
											}
											x3mw3=xmw1-joumw(x1mw1,0.5);
											d8mw=joumw(joumw(x3mw3-x1mw[imw-1],2)+joumw(y2mwI-y1mw[imw-1],2),0.5)/w1mw[imw-1];
											for(kmw=1;kmw<=Nmw;kmw++){
													if(kmw!=imw && kmw!=jmw){
															d9mw=joumw(joumw(x3mw3-x1mw[kmw-1],2)+joumw(y2mwI-y1mw[kmw-1],2),0.5)/w1mw[kmw-1];
															if(d8mw>d9mw){
																	br4mw=1;
																	break;
															}
													}//if kmw!=imw...
											}//next kmw
											if(br4mw==0){
													x3mwI=(int)(x3mw3+0.5);
													g.drawLine(x3mwI,y2mwI,x3mwI,y2mwI);
											}//if br1==0
											else{//br1mw==1
													br4mw=0;
											}
									}//if x2mwI>=0....
							}// next x2mwI
					}//next jmw
			}//next imw
	}
}
