# 종가매매 종목 추출 및 검증 프로그램

## 개요
- 본 프로젝트는 주식 종가 데이터를 기반으로 특정 종목을 선정하고, 선정된 종목의 수익 여부를 분석하는 프로그램입니다. 
- 스터디 및 실험적 구현을 위해 Scala, ZIO 기반으로 개발되었습니다.

### 주요 기능
- 종목 가격, 거래량의 Slow Crawling 적용 (대량 트래픽으로 인한 차단 방지)
- 종가매매 종목 추출을 위한 추출 알고리즘 적용 (매일 15:10 금일자 대상종목 추출)
- 종목 추출후 다음날 적중 여부 검증, 추출후 5일간 변화도 분석

## 설치 및 실행 방법
- TBD

### Demo
- [http://heyes.live:443/stock/closingPriceAnalysis](http://heyes.live:443/stock/closingPriceAnalysis)
- [https://www.heyes.live/stock/closingPriceAnalysis](https://www.heyes.live/stock/closingPriceAnalysis)
